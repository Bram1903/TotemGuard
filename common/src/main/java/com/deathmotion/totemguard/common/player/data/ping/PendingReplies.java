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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

final class PendingReplies {

    private final Deque<PendingReply> pending = new ArrayDeque<>();
    private final Map<Integer, PendingReply> stagedTransactions = new HashMap<>();
    private int lastPositiveTransactionId;
    private int acceptedTransactions;
    private int acceptedSyntheticTransactions;

    void sendKeepAlive(long id, long timestamp) {
        pending.addLast(new PendingReply(ReplyType.KEEP_ALIVE, id, timestamp));
    }

    int reserveTransactionId(int maxPositiveId) {
        if (maxPositiveId < 1) {
            throw new IllegalArgumentException("maxPositiveId must be positive");
        }

        int nextTransactionId = lastPositiveTransactionId >= maxPositiveId ? 1 : lastPositiveTransactionId + 1;
        this.lastPositiveTransactionId = nextTransactionId;
        return nextTransactionId;
    }

    void addTransactionCallback(int id, LongConsumer callback) {
        stagedTransaction(id).addCallback(callback);
    }

    void sendTransaction(int id, long timestamp) {
        if (id > 0) {
            this.lastPositiveTransactionId = id;
        }

        PendingReply reply = stagedTransactions.remove(id);
        if (reply == null) {
            reply = new PendingReply(ReplyType.TRANSACTION, id);
        }

        reply.setSentAt(timestamp);
        pending.addLast(reply);
    }

    void markTransactionSynthetic(int id) {
        stagedTransaction(id).setSynthetic(true);
    }

    void sendTeleport(int id, long timestamp) {
        pending.addLast(new PendingReply(ReplyType.TELEPORT, id, timestamp));
    }

    ReplyResult receive(ReplyType type, long id, long timestamp) {
        ReplyMatch match = match(type, id);
        if (match == null) {
            return ReplyResult.invalid();
        }

        accept(match.entries(), timestamp);
        int ping = match.matched().sentAt() == null ? PingData.INVALID_PING : PingData.clampPing(timestamp - match.matched().sentAt());
        return new ReplyResult(true, match.skippedCount() > 0, match.skippedCount(), ping, match.matched().synthetic());
    }

    int pendingTransactions() {
        int count = 0;

        for (PendingReply reply : pending) {
            if (reply.type() == ReplyType.TRANSACTION) {
                count++;
            }
        }

        return count;
    }

    int pendingSyntheticTransactions() {
        int count = 0;

        for (PendingReply reply : pending) {
            if (reply.type() == ReplyType.TRANSACTION && reply.synthetic()) {
                count++;
            }
        }

        return count;
    }

    int acceptedTransactions() {
        return acceptedTransactions;
    }

    int acceptedSyntheticTransactions() {
        return acceptedSyntheticTransactions;
    }

    private PendingReply stagedTransaction(int id) {
        PendingReply reply = stagedTransactions.get(id);
        if (reply != null) {
            return reply;
        }

        reply = new PendingReply(ReplyType.TRANSACTION, id);
        stagedTransactions.put(id, reply);
        return reply;
    }

    private ReplyMatch match(ReplyType type, long id) {
        List<PendingReply> entries = new ArrayList<>();
        PendingReply matched = null;

        for (PendingReply reply : pending) {
            entries.add(reply);
            if (reply.matches(type, id)) {
                matched = reply;
                break;
            }
        }

        if (matched == null) {
            return null;
        }

        for (int i = 0; i < entries.size(); i++) {
            pending.removeFirst();
        }

        return new ReplyMatch(matched, entries);
    }

    private void accept(List<PendingReply> entries, long timestamp) {
        for (PendingReply reply : entries) {
            if (reply.type() == ReplyType.TRANSACTION) {
                this.acceptedTransactions++;
                if (reply.synthetic()) {
                    this.acceptedSyntheticTransactions++;
                }
            }

            reply.runCallbacks(timestamp);
        }
    }
}
