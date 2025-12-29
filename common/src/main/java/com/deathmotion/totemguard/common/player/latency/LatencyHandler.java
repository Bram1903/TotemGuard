/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.common.player.latency;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class LatencyHandler {

    private static final int NAMESPACE = 0xA;
    private static final int COUNTER_MASK = 0x07FF;

    private final TGPlayer player;
    private final Object lock = new Object();


    private InFlightBarrier inFlight;
    private int seq = ThreadLocalRandom.current().nextInt(0, 1 << 11);

    @Getter
    private volatile long lastAckAtMillis;

    @Getter
    private volatile long lastRttNanos;

    public LatencyHandler(TGPlayer player) {
        this.player = player;
    }

    public void afterNextAckDelayed(PacketSendEvent event, Runnable task) {
        afterNextAckAfterSendInternal(event, task);
    }

    public void afterNextAckDelayedAsync(PacketSendEvent event, Runnable task) {
        ChannelHelper.runInEventLoop(player.getUser().getChannel(), () -> afterNextAckAfterSendInternal(event, task));
    }

    public void afterNextAck(Runnable task) {
        afterNextAckSendNowInternal(task);
    }

    public void afterNextAckAsync(Runnable task) {
        ChannelHelper.runInEventLoop(player.getUser().getChannel(), () -> afterNextAckSendNowInternal(task));
    }

    private void afterNextAckAfterSendInternal(PacketSendEvent event, Runnable task) {
        if (task == null) return;

        if (event == null) {
            afterNextAckSendNowInternal(task);
            return;
        }

        if (player.getUser().getEncoderState() != ConnectionState.PLAY) return;
        if (event.isCancelled()) return;

        final short idToSend;
        final int ticketToSend;

        synchronized (lock) {
            if (inFlight == null) {
                inFlight = new InFlightBarrier(nextTotemGuardId());
            }

            inFlight.callbacks.add(task);

            if (inFlight.sentAtNanos != 0L) {
                return;
            }

            inFlight.sendTicket++;
            idToSend = inFlight.id;
            ticketToSend = inFlight.sendTicket;
        }

        event.getTasksAfterSend().add(() -> sendTransactionPacketOnNettyThreadIfLatest(idToSend, ticketToSend));
    }

    private void afterNextAckSendNowInternal(Runnable task) {
        if (task == null) return;
        if (player.getUser().getEncoderState() != ConnectionState.PLAY) return;

        final short idToSend;
        boolean shouldSend = false;

        synchronized (lock) {
            if (inFlight == null) {
                inFlight = new InFlightBarrier(nextTotemGuardId());
                idToSend = inFlight.id;
                shouldSend = true;
            } else {
                idToSend = 0;
            }

            inFlight.callbacks.add(task);

            if (inFlight.sentAtNanos != 0L) {
                shouldSend = false;
            }
        }

        if (shouldSend) {
            sendTransactionPacketOnNettyThread(idToSend);
        }
    }

    private short nextTotemGuardId() {
        int counter = (seq++) & COUNTER_MASK;

        if (NAMESPACE == 0xF && counter == COUNTER_MASK) {
            counter = (seq++) & COUNTER_MASK;
        }

        int raw = 0x8000 | (NAMESPACE << 11) | counter;
        return (short) raw;
    }

    private void sendTransactionPacketOnNettyThread(short id) {
        try {
            final PacketWrapper<?> packet;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                packet = new WrapperPlayServerPing(id);
            } else {
                packet = new WrapperPlayServerWindowConfirmation((byte) 0, id, false);
            }

            synchronized (lock) {
                if (inFlight == null || inFlight.id != id) return;

                if (inFlight.sentAtNanos != 0L) return;
                inFlight.sentAtNanos = System.nanoTime();
            }

            player.getUser().writePacket(packet);

        } catch (Throwable t) {
            synchronized (lock) {
                inFlight = null;
            }
        }
    }

    private void sendTransactionPacketOnNettyThreadIfLatest(short id, int ticket) {
        try {
            final PacketWrapper<?> packet;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                packet = new WrapperPlayServerPing(id);
            } else {
                packet = new WrapperPlayServerWindowConfirmation((byte) 0, id, false);
            }

            synchronized (lock) {
                if (inFlight == null || inFlight.id != id) return;

                if (inFlight.sentAtNanos != 0L) return;
                if (inFlight.sendTicket != ticket) return;
                inFlight.sentAtNanos = System.nanoTime();
            }

            player.getUser().writePacket(packet);

        } catch (Throwable t) {
            synchronized (lock) {
                inFlight = null;
            }
        }
    }

    public boolean onTransactionResponse(short id) {
        final List<Runnable> toRun;
        final long sentNano;

        synchronized (lock) {
            if (inFlight == null || inFlight.id != id) {
                return false;
            }

            if (inFlight.sentAtNanos == 0L) {
                return false;
            }

            sentNano = inFlight.sentAtNanos;
            toRun = new ArrayList<>(inFlight.callbacks);
            inFlight = null;
        }

        lastRttNanos = System.nanoTime() - sentNano;
        lastAckAtMillis = System.currentTimeMillis();

        TGPlatform.getInstance().getLogger().info("Received latency packet " + id + " for " + player.getUser().getName() + " in " + getLastRttMillis() + "ms");

        for (Runnable r : toRun) {
            try {
                r.run();
            } catch (Throwable t) {
                TGPlatform.getInstance().getLogger().log(Level.SEVERE, "Error running latency callbacks for " + player.getUser().getName(), t);
            }
        }

        return true;
    }

    public long getLastRttMillis() {
        return TimeUnit.NANOSECONDS.toMillis(lastRttNanos);
    }

    private static final class InFlightBarrier {
        final short id;
        final List<Runnable> callbacks = new ArrayList<>(2);

        long sentAtNanos;
        int sendTicket;

        InFlightBarrier(short id) {
            this.id = id;
        }
    }
}
