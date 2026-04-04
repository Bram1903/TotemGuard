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

package com.deathmotion.totemguard.common.player.data;

import lombok.Getter;

import java.util.*;

public class PingData {

    private static final int INVALID_PING = -1;

    private final PendingPingTracker<Long> keepAliveTracker = new PendingPingTracker<>();
    private final PendingPingTracker<Integer> transactionTracker = new PendingPingTracker<>();

    @Getter
    private int keepAlivePing = INVALID_PING;
    @Getter
    private int transactionPing = INVALID_PING;

    private static int clampPing(long ping) {
        if (ping < 0L || ping > Integer.MAX_VALUE) {
            return INVALID_PING;
        }

        return (int) ping;
    }

    public void keepAliveSent(long id, long timestamp) {
        keepAliveTracker.sent(id, timestamp);
    }

    public void transactionSent(int id, long timestamp) {
        transactionTracker.sent(id, timestamp);
    }

    public void keepAliveReceived(long id, long timestamp) {
        this.keepAlivePing = keepAliveTracker.received(id, timestamp);
    }

    public void transactionReceived(int id, long timestamp) {
        this.transactionPing = transactionTracker.received(id, timestamp);
    }

    private static final class PendingPingTracker<K> {

        private final Map<K, Long> sentPackets = new LinkedHashMap<>();

        private void sent(K id, long timestamp) {
            sentPackets.put(id, timestamp);
        }

        private int received(K id, long timestamp) {
            MatchedPacket<K> matchedPacket = consumeMatchedPacket(id);
            if (matchedPacket == null) {
                return INVALID_PING;
            }

            discardSkippedPackets(matchedPacket.skippedPacketIds());
            return clampPing(timestamp - matchedPacket.sentAt());
        }

        private MatchedPacket<K> consumeMatchedPacket(K id) {
            List<K> skippedPacketIds = new ArrayList<>();
            Iterator<Map.Entry<K, Long>> pendingPackets = sentPackets.entrySet().iterator();

            // Acknowledgements are handled in send order. Once a newer packet is
            // accepted, every older pending packet before it is stale.
            while (pendingPackets.hasNext()) {
                Map.Entry<K, Long> pendingPacket = pendingPackets.next();

                if (pendingPacket.getKey().equals(id)) {
                    pendingPackets.remove();
                    return new MatchedPacket<>(pendingPacket.getValue(), skippedPacketIds);
                }

                skippedPacketIds.add(pendingPacket.getKey());
            }

            return null;
        }

        private void discardSkippedPackets(List<K> skippedPacketIds) {
            for (K skippedPacketId : skippedPacketIds) {
                sentPackets.remove(skippedPacketId);
            }
        }

        private record MatchedPacket<K>(long sentAt, List<K> skippedPacketIds) {
        }
    }
}
