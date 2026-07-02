/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.ping.PingData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

public class PacketLatencyHandler {

    private final TGPlayer player;
    private final PingData pingData;

    public PacketLatencyHandler(TGPlayer player) {
        this.player = player;
        this.pingData = player.getPingData();
    }

    public void compensate(PacketSendEvent event, Runnable callback) {
        compensate(event, timestamp -> callback.run());
    }

    public void compensate(PacketSendEvent event, LongConsumer callback) {
        if (event.isCancelled()) return;
        findOrCreateTrackingTask(event, true).addCallback(callback);
    }

    public void compensateLazy(PacketSendEvent event, Runnable callback) {
        compensateLazy(event, timestamp -> callback.run());
    }

    public void compensateLazy(PacketSendEvent event, LongConsumer callback) {
        if (event.isCancelled()) return;
        findOrCreateTrackingTask(event, false).addCallback(callback);
    }

    public void sendTransaction(LongConsumer callback) {
        if (callback == null) return;
        runOnEventLoop(() -> {
            pingData.stageForNextTransaction(List.of(callback));
            flushTransaction();
        });
    }

    public void sendHeartbeat() {
        runOnEventLoop(this::flushTransaction);
    }

    private void runOnEventLoop(Runnable action) {
        Object channel = player.getUser().getChannel();
        if (channel == null || !ChannelHelper.isOpen(channel)) return;
        ChannelHelper.runInEventLoop(channel, action);
    }

    private PendingPacketLatencyTask findOrCreateTrackingTask(PacketSendEvent event, boolean urgent) {
        for (Runnable taskAfterSend : event.getTasksAfterSend()) {
            if (taskAfterSend instanceof PendingPacketLatencyTask pendingPacketLatencyTask) {
                if (urgent) {
                    pendingPacketLatencyTask.urgent = true;
                }
                return pendingPacketLatencyTask;
            }
        }

        PendingPacketLatencyTask pendingPacketLatencyTask =
                new PendingPacketLatencyTask(event, pingData.getTransactionSendSequence(), urgent);
        event.getTasksAfterSend().add(pendingPacketLatencyTask);
        return pendingPacketLatencyTask;
    }

    private void flushTransaction() {
        if (player.getUser().getEncoderState() != ConnectionState.PLAY) return;

        int transactionId = pingData.reserveNextTransactionId(maxTransactionId());
        pingData.markTransactionSynthetic(transactionId);
        player.getUser().sendPacket(createTransactionPacket(transactionId));
    }

    private PacketWrapper<?> createTransactionPacket(int transactionId) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            return new WrapperPlayServerPing(transactionId);
        }

        return new WrapperPlayServerWindowConfirmation(0, (short) transactionId, false);
    }

    private int maxTransactionId() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            return Integer.MAX_VALUE;
        }

        return Short.MAX_VALUE;
    }

    private final class PendingPacketLatencyTask implements Runnable {

        private final PacketSendEvent event;
        private final long sequenceAtRegistration;
        private final List<LongConsumer> callbacks = new ArrayList<>(2);
        private boolean urgent;

        private PendingPacketLatencyTask(PacketSendEvent event, long sequenceAtRegistration, boolean urgent) {
            this.event = event;
            this.sequenceAtRegistration = sequenceAtRegistration;
            this.urgent = urgent;
        }

        private void addCallback(LongConsumer callback) {
            callbacks.add(callback);
        }

        @Override
        public void run() {
            if (event.isCancelled() || callbacks.isEmpty()) return;
            if (pingData.attachSinceTransactionSequence(sequenceAtRegistration, callbacks)) return;

            pingData.stageForNextTransaction(callbacks);
            if (urgent) {
                flushTransaction();
            }
        }
    }
}
