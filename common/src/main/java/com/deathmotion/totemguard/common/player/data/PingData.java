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

import com.deathmotion.totemguard.common.TGPlatform;
import lombok.Getter;

import java.util.*;
import java.util.function.LongConsumer;
import java.util.logging.Level;

public class PingData {

    private static final int INVALID_PING = -1;

    private final PendingPingTracker<Long> keepAliveTracker = new PendingPingTracker<>();
    private final PendingTransactionTracker transactionTracker = new PendingTransactionTracker();

    @Getter
    private int keepAlivePing = INVALID_PING;
    @Getter
    private int transactionPing = INVALID_PING;
    @Getter
    private boolean observedTransactionReply;
    @Getter
    private boolean observedTeleportTransactionResult;
    @Getter
    private boolean lastTransactionReplyValid;
    @Getter
    private boolean lastTransactionReplySynthetic;
    @Getter
    private boolean lastTransactionReplySkipped;
    @Getter
    private int lastSkippedTransactionReplyCount;
    @Getter
    private boolean lastTeleportSkippedTransactions;
    @Getter
    private int lastSkippedTransactionsByTeleportCount;

    private static int clampPing(long ping) {
        if (ping < 0L || ping > Integer.MAX_VALUE) {
            return INVALID_PING;
        }

        return (int) ping;
    }

    public void keepAliveSent(long id, long timestamp) {
        keepAliveTracker.sent(id, timestamp);
    }

    public int reserveNextTransactionId(int maxPositiveId) {
        return transactionTracker.reserveNextPositiveId(maxPositiveId);
    }

    public void addTransactionCallback(int id, LongConsumer callback) {
        transactionTracker.addCallback(id, callback);
    }

    public void transactionSent(int id, long timestamp) {
        transactionTracker.sent(id, timestamp, false);
    }

    public void markTransactionSynthetic(int id) {
        transactionTracker.markSynthetic(id);
    }

    public boolean shouldCancelTransactionReplyOnProxy() {
        return lastTransactionReplySynthetic;
    }

    public int getPendingTransactionCount() {
        return transactionTracker.pendingCount();
    }

    public int getPendingSyntheticTransactionCount() {
        return transactionTracker.pendingSyntheticCount();
    }

    public int getAcceptedTransactionCount() {
        return transactionTracker.acceptedCount();
    }

    public int getAcceptedSyntheticTransactionCount() {
        return transactionTracker.acceptedSyntheticCount();
    }

    public void trackTeleport(int teleportId) {
        transactionTracker.trackTeleport(teleportId);
    }

    public void keepAliveReceived(long id, long timestamp) {
        this.keepAlivePing = keepAliveTracker.received(id, timestamp);
    }

    public void transactionReceived(int id, long timestamp) {
        TransactionReplyObservation observation = transactionTracker.received(id, timestamp);
        this.observedTransactionReply = true;
        this.transactionPing = observation.ping();
        this.lastTransactionReplyValid = observation.valid();
        this.lastTransactionReplySynthetic = observation.synthetic();
        this.lastTransactionReplySkipped = observation.skipped();
        this.lastSkippedTransactionReplyCount = observation.skippedCount();
    }

    public void teleportReceived(int teleportId, long timestamp) {
        TeleportTransactionObservation observation = transactionTracker.teleportReceived(teleportId, timestamp);
        this.observedTeleportTransactionResult = true;
        this.lastTeleportSkippedTransactions = observation.skipped();
        this.lastSkippedTransactionsByTeleportCount = observation.skippedCount();
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

    private static final class PendingTransactionTracker {

        private final Deque<PendingTransaction> pendingTransactions = new ArrayDeque<>();
        private final Map<Integer, PendingTransaction> pendingTeleports = new HashMap<>();
        private int lastPositiveTransactionId;
        private int acceptedTransactions;
        private int acceptedSyntheticTransactions;

        private int reserveNextPositiveId(int maxPositiveId) {
            if (maxPositiveId < 1) {
                throw new IllegalArgumentException("maxPositiveId must be positive");
            }

            int nextTransactionId = lastPositiveTransactionId >= maxPositiveId ? 1 : lastPositiveTransactionId + 1;
            this.lastPositiveTransactionId = nextTransactionId;
            return nextTransactionId;
        }

        private void addCallback(int id, LongConsumer callback) {
            PendingTransaction pendingTransaction = findLatestPendingTransactionWithoutTimestamp(id);
            if (pendingTransaction == null) {
                pendingTransaction = new PendingTransaction(id);
                pendingTransactions.addLast(pendingTransaction);
            }

            pendingTransaction.addCallback(callback);
        }

        private void sent(int id, long timestamp, boolean synthetic) {
            if (id > 0) {
                this.lastPositiveTransactionId = id;
            }

            PendingTransaction pendingTransaction = findLatestPendingTransactionWithoutTimestamp(id);
            if (pendingTransaction == null) {
                pendingTransaction = new PendingTransaction(id);
                pendingTransactions.addLast(pendingTransaction);
            }

            pendingTransaction.setSentAt(timestamp);
            pendingTransaction.setSynthetic(pendingTransaction.isSynthetic() || synthetic);
        }

        private void markSynthetic(int id) {
            PendingTransaction pendingTransaction = findLatestPendingTransaction(id);
            if (pendingTransaction == null) {
                pendingTransaction = new PendingTransaction(id);
                pendingTransactions.addLast(pendingTransaction);
            }

            pendingTransaction.setSynthetic(true);
        }

        private void trackTeleport(int teleportId) {
            PendingTransaction transactionBoundary = pendingTransactions.peekLast();
            if (transactionBoundary != null) {
                pendingTeleports.put(teleportId, transactionBoundary);
            }
        }

        private TransactionReplyObservation received(int id, long timestamp) {
            MatchedTransaction matchedTransaction = consumeMatchedTransactionReply(id);
            if (matchedTransaction == null) {
                return new TransactionReplyObservation(false, false, 0, INVALID_PING, false);
            }

            recordAcceptedTransactions(matchedTransaction.acceptedTransactions());
            matchedTransaction.runCallbacks(timestamp);

            Long sentAt = matchedTransaction.matchedPacket().getSentAt();
            if (sentAt == null) {
                return new TransactionReplyObservation(
                        true,
                        matchedTransaction.hasSkippedTransactions(),
                        matchedTransaction.skippedCount(),
                        INVALID_PING,
                        matchedTransaction.matchedPacket().isSynthetic()
                );
            }

            return new TransactionReplyObservation(
                    true,
                    matchedTransaction.hasSkippedTransactions(),
                    matchedTransaction.skippedCount(),
                    clampPing(timestamp - sentAt),
                    matchedTransaction.matchedPacket().isSynthetic()
            );
        }

        private TeleportTransactionObservation teleportReceived(int teleportId, long timestamp) {
            PendingTransaction transactionBoundary = pendingTeleports.remove(teleportId);
            if (transactionBoundary == null) {
                return TeleportTransactionObservation.NONE;
            }

            List<PendingTransaction> skippedTransactions = new ArrayList<>();

            while (!pendingTransactions.isEmpty()) {
                PendingTransaction pendingTransaction = pendingTransactions.removeFirst();
                skippedTransactions.add(pendingTransaction);

                if (pendingTransaction == transactionBoundary) {
                    recordAcceptedTransactions(skippedTransactions);
                    for (PendingTransaction skippedTransaction : skippedTransactions) {
                        skippedTransaction.runCallbacks(timestamp);
                    }

                    return new TeleportTransactionObservation(true, skippedTransactions.size());
                }
            }

            return TeleportTransactionObservation.NONE;
        }

        private MatchedTransaction consumeMatchedTransactionReply(int id) {
            List<PendingTransaction> skippedPackets = new ArrayList<>();
            Iterator<PendingTransaction> pendingPackets = pendingTransactions.iterator();

            while (pendingPackets.hasNext()) {
                PendingTransaction pendingPacket = pendingPackets.next();

                if (pendingPacket.getId() == id) {
                    pendingPackets.remove();
                    discardSkippedTransactions(skippedPackets.size());
                    return new MatchedTransaction(pendingPacket, skippedPackets);
                }

                skippedPackets.add(pendingPacket);
            }

            return null;
        }

        private PendingTransaction findLatestPendingTransactionWithoutTimestamp(int id) {
            Iterator<PendingTransaction> pendingTransactions = this.pendingTransactions.descendingIterator();
            while (pendingTransactions.hasNext()) {
                PendingTransaction pendingTransaction = pendingTransactions.next();
                if (pendingTransaction.getId() == id && pendingTransaction.getSentAt() == null) {
                    return pendingTransaction;
                }
            }

            return null;
        }

        private PendingTransaction findLatestPendingTransaction(int id) {
            Iterator<PendingTransaction> pendingTransactions = this.pendingTransactions.descendingIterator();
            while (pendingTransactions.hasNext()) {
                PendingTransaction pendingTransaction = pendingTransactions.next();
                if (pendingTransaction.getId() == id) {
                    return pendingTransaction;
                }
            }

            return null;
        }

        private void discardSkippedTransactions(int skippedCount) {
            for (int i = 0; i < skippedCount; i++) {
                pendingTransactions.removeFirst();
            }
        }

        private int pendingCount() {
            return pendingTransactions.size();
        }

        private int pendingSyntheticCount() {
            int syntheticTransactions = 0;

            for (PendingTransaction pendingTransaction : pendingTransactions) {
                if (pendingTransaction.isSynthetic()) {
                    syntheticTransactions++;
                }
            }

            return syntheticTransactions;
        }

        private int pendingTeleportCount() {
            return pendingTeleports.size();
        }

        private int acceptedCount() {
            return acceptedTransactions;
        }

        private int acceptedSyntheticCount() {
            return acceptedSyntheticTransactions;
        }

        private void recordAcceptedTransactions(Collection<PendingTransaction> acceptedTransactions) {
            this.acceptedTransactions += acceptedTransactions.size();

            for (PendingTransaction acceptedTransaction : acceptedTransactions) {
                if (acceptedTransaction.isSynthetic()) {
                    this.acceptedSyntheticTransactions++;
                }
            }
        }

        private record MatchedTransaction(
                PendingTransaction matchedPacket,
                List<PendingTransaction> skippedPackets
        ) {

            private boolean hasSkippedTransactions() {
                return !skippedPackets.isEmpty();
            }

            private int skippedCount() {
                return skippedPackets.size();
            }

            private List<PendingTransaction> acceptedTransactions() {
                List<PendingTransaction> acceptedTransactions = new ArrayList<>(skippedPackets.size() + 1);
                acceptedTransactions.addAll(skippedPackets);
                acceptedTransactions.add(matchedPacket);
                return acceptedTransactions;
            }

            private void runCallbacks(long timestamp) {
                for (PendingTransaction skippedPacket : skippedPackets) {
                    skippedPacket.runCallbacks(timestamp);
                }

                matchedPacket.runCallbacks(timestamp);
            }
        }
    }

    private static final class PendingTransaction {

        @Getter
        private final int id;
        private final List<LongConsumer> callbacks = new ArrayList<>();
        @Getter
        private Long sentAt;
        @Getter
        private boolean synthetic;

        private PendingTransaction(int id) {
            this.id = id;
        }

        private void addCallback(LongConsumer callback) {
            callbacks.add(callback);
        }

        private void setSentAt(long timestamp) {
            this.sentAt = timestamp;
        }

        private void setSynthetic(boolean synthetic) {
            this.synthetic = synthetic;
        }

        private void runCallbacks(long timestamp) {
            for (LongConsumer callback : callbacks) {
                try {
                    callback.accept(timestamp);
                } catch (Exception exception) {
                    TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to execute transaction callback.", exception);
                }
            }
        }
    }

    private record TransactionReplyObservation(boolean valid, boolean skipped, int skippedCount, int ping,
                                               boolean synthetic) {
    }

    private record TeleportTransactionObservation(boolean skipped, int skippedCount) {

        private static final TeleportTransactionObservation NONE = new TeleportTransactionObservation(false, 0);
    }
}
