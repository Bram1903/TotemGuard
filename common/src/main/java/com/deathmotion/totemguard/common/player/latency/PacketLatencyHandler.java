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
import com.deathmotion.totemguard.common.player.data.PingData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
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

        findOrCreateTrackingTask(event).addCallback(callback);
    }

    private PendingPacketLatencyTask findOrCreateTrackingTask(PacketSendEvent event) {
        for (Runnable taskAfterSend : event.getTasksAfterSend()) {
            if (taskAfterSend instanceof PendingPacketLatencyTask pendingPacketLatencyTask) {
                return pendingPacketLatencyTask;
            }
        }

        PendingPacketLatencyTask pendingPacketLatencyTask = new PendingPacketLatencyTask();
        event.getTasksAfterSend().add(pendingPacketLatencyTask);
        return pendingPacketLatencyTask;
    }

    private void sendTransactionPacket(List<LongConsumer> callbacks) {
        int transactionId = pingData.reserveNextTransactionId(maxTransactionId());
        for (LongConsumer callback : callbacks) {
            pingData.addTransactionCallback(transactionId, callback);
        }

        player.getUser().sendPacketSilently(createTransactionPacket(transactionId));
        pingData.syntheticTransactionSent(transactionId, System.currentTimeMillis());
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

        private final List<LongConsumer> callbacks = new ArrayList<>();

        private void addCallback(LongConsumer callback) {
            callbacks.add(callback);
        }

        @Override
        public void run() {
            sendTransactionPacket(callbacks);
        }
    }
}
