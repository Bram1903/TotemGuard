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

package com.deathmotion.totemguard.common.player.data.ping;

import java.util.*;
import java.util.function.LongConsumer;

final class PendingTransactions {

    private final Deque<PendingTransaction> pending = new ArrayDeque<>();
    private final Map<Integer, PendingTransaction> staged = new HashMap<>();
    private final Map<Integer, PendingTransaction> teleportBoundaries = new HashMap<>();
    private int lastPositiveTransactionId;
    private volatile int pendingCount;
    private volatile int pendingSyntheticCount;
    private volatile int acceptedTransactions;
    private volatile int acceptedSyntheticTransactions;
    private volatile long oldestPendingSentAt;

    int reserveId(int maxPositiveId) {
        if (maxPositiveId < 1) {
            throw new IllegalArgumentException("maxPositiveId must be positive");
        }

        int nextTransactionId = lastPositiveTransactionId >= maxPositiveId ? 1 : lastPositiveTransactionId + 1;
        this.lastPositiveTransactionId = nextTransactionId;
        return nextTransactionId;
    }

    void addCallback(int id, LongConsumer callback) {
        staged(id).addCallback(callback);
    }

    void sent(int id, long timestamp) {
        if (id > 0) {
            this.lastPositiveTransactionId = id;
        }

        PendingTransaction transaction = staged.remove(id);
        if (transaction == null) {
            transaction = new PendingTransaction(id);
        }

        transaction.setSentAt(timestamp);
        pending.addLast(transaction);
        pendingCount++;
        if (transaction.synthetic()) {
            pendingSyntheticCount++;
        }
        if (oldestPendingSentAt == 0L) {
            oldestPendingSentAt = timestamp;
        }
    }

    void markSynthetic(int id) {
        staged(id).setSynthetic(true);
    }

    void trackTeleport(int teleportId) {
        PendingTransaction boundary = pending.peekLast();
        if (boundary != null) {
            teleportBoundaries.put(teleportId, boundary);
        }
    }

    PingReplyResult receive(int id, long timestamp) {
        TransactionMatch match = match(id);
        if (match == null) {
            return PingReplyResult.invalid();
        }

        List<PendingTransaction> accepted = match.accepted();
        recordAccepted(accepted);
        PingReplyResult result = new PingReplyResult(
                true,
                match.skippedCount() > 0,
                match.skippedCount(),
                match.matched().sentAt() == null ? PingData.INVALID_PING : PingData.clampPing(timestamp - match.matched().sentAt()),
                match.matched().synthetic()
        );
        runCallbacks(accepted, timestamp);
        return result;
    }

    TeleportReplyResult receiveTeleport(int teleportId, long timestamp) {
        PendingTransaction boundary = teleportBoundaries.remove(teleportId);
        if (boundary == null) {
            return TeleportReplyResult.none();
        }

        List<PendingTransaction> accepted = new ArrayList<>(2);
        boolean found = false;
        final int size = pending.size();
        for (int i = 0; i < size; i++) {
            PendingTransaction transaction = pending.pollFirst();
            accepted.add(transaction);
            if (transaction == boundary) {
                found = true;
                break;
            }
        }

        if (!found) {
            for (int i = accepted.size() - 1; i >= 0; i--) {
                pending.addFirst(accepted.get(i));
            }
            return TeleportReplyResult.none();
        }

        recordAccepted(accepted);
        runCallbacks(accepted, timestamp);
        return new TeleportReplyResult(!accepted.isEmpty(), accepted.size());
    }

    int pendingCount() {
        return pendingCount;
    }

    int pendingSyntheticCount() {
        return pendingSyntheticCount;
    }

    int acceptedCount() {
        return acceptedTransactions;
    }

    int acceptedSyntheticCount() {
        return acceptedSyntheticTransactions;
    }

    long oldestPendingSentAt() {
        return oldestPendingSentAt;
    }

    private PendingTransaction staged(int id) {
        PendingTransaction transaction = staged.get(id);
        if (transaction != null) {
            return transaction;
        }

        transaction = new PendingTransaction(id);
        staged.put(id, transaction);
        return transaction;
    }

    private TransactionMatch match(int id) {
        // Drain from the head of the deque until we find the matching id. Avoids the
        // ArrayDeque iterator allocation that for-each would produce. If we never find a
        // match (rare — transactions are normally acked in order), the drained entries
        // are restored in their original order.
        List<PendingTransaction> accepted = new ArrayList<>(2);
        PendingTransaction matched = null;
        final int size = pending.size();
        for (int i = 0; i < size; i++) {
            PendingTransaction transaction = pending.pollFirst();
            accepted.add(transaction);
            if (transaction.id() == id) {
                matched = transaction;
                break;
            }
        }

        if (matched == null) {
            for (int i = accepted.size() - 1; i >= 0; i--) {
                pending.addFirst(accepted.get(i));
            }
            return null;
        }

        return new TransactionMatch(matched, accepted);
    }

    private void recordAccepted(List<PendingTransaction> accepted) {
        final int size = accepted.size();
        pendingCount -= size;

        for (int i = 0; i < size; i++) {
            PendingTransaction transaction = accepted.get(i);
            this.acceptedTransactions++;
            if (transaction.synthetic()) {
                pendingSyntheticCount--;
                this.acceptedSyntheticTransactions++;
            }
        }

        PendingTransaction head = pending.peekFirst();
        if (head == null) {
            oldestPendingSentAt = 0L;
        } else {
            Long sent = head.sentAt();
            oldestPendingSentAt = sent == null ? 0L : sent;
        }
    }

    private void runCallbacks(List<PendingTransaction> accepted, long timestamp) {
        final int size = accepted.size();
        for (int i = 0; i < size; i++) {
            accepted.get(i).runCallbacks(timestamp);
        }
    }
}
