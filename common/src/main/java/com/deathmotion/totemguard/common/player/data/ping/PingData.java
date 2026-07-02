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

import lombok.Getter;

import java.util.List;
import java.util.function.LongConsumer;

public class PingData {

    static final int INVALID_PING = -1;

    private final PendingKeepAlives keepAlives = new PendingKeepAlives();
    private final PendingTransactions transactions = new PendingTransactions();

    @Getter
    private int keepAlivePing = INVALID_PING;
    @Getter
    private int transactionPing = INVALID_PING;
    @Getter
    private boolean observedTransactionReply;
    @Getter
    private boolean observedKeepAliveReply;
    @Getter
    private boolean observedTeleportReply;
    @Getter
    private boolean lastTransactionReplyValid;
    @Getter
    private boolean lastTransactionReplySynthetic;
    @Getter
    private boolean lastTransactionReplySkipped;
    @Getter
    private int lastSkippedTransactionReplyCount;
    @Getter
    private boolean lastKeepAliveReplyValid;
    @Getter
    private boolean lastKeepAliveReplySkipped;
    @Getter
    private int lastSkippedKeepAliveReplyCount;
    @Getter
    private boolean lastTeleportReplySkipped;
    @Getter
    private int lastSkippedTeleportReplyCount;

    static int clampPing(long ping) {
        if (ping < 0L || ping > Integer.MAX_VALUE) {
            return INVALID_PING;
        }

        return (int) ping;
    }

    public void keepAliveSent(long id, long timestamp) {
        keepAlives.send(id, timestamp);
    }

    public int reserveNextTransactionId(int maxPositiveId) {
        return transactions.reserveId(maxPositiveId);
    }

    public long getTransactionSendSequence() {
        return transactions.sendSequence();
    }

    public void stageForNextTransaction(List<LongConsumer> callbacks) {
        transactions.stageForNext(callbacks);
    }

    public boolean attachSinceTransactionSequence(long sequence, List<LongConsumer> callbacks) {
        return transactions.attachSince(sequence, callbacks);
    }

    public void transactionSent(int id, long timestamp) {
        transactions.sent(id, timestamp);
    }

    public void markTransactionSynthetic(int id) {
        transactions.markSynthetic(id);
    }

    public int getPendingTransactionCount() {
        return transactions.pendingCount();
    }

    public int getPendingSyntheticTransactionCount() {
        return transactions.pendingSyntheticCount();
    }

    public int getAcceptedTransactionCount() {
        return transactions.acceptedCount();
    }

    public int getAcceptedSyntheticTransactionCount() {
        return transactions.acceptedSyntheticCount();
    }

    public long getOldestPendingTransactionSentAt() {
        return transactions.oldestPendingSentAt();
    }

    public long getOldestPendingSyntheticTransactionSentAt() {
        return transactions.oldestPendingSyntheticSentAt();
    }

    public long getLastTransactionSentNanos() {
        return transactions.lastSentNanos();
    }

    public long getLastThirdPartyTransactionSentNanos() {
        return transactions.lastThirdPartySentNanos();
    }

    public long getLastAckedTransactionSentNanos() {
        return transactions.lastAckedSentNanos();
    }

    public long getLastMatchedTransactionAckNanos() {
        return transactions.lastMatchedAckNanos();
    }

    public void markMovementForTransactionAnchor() {
        transactions.markMovement();
    }

    public long getGatedTransactionAnchorNanos() {
        return transactions.gatedAnchorNanos();
    }

    public void teleportSent(int teleportId) {
        transactions.trackTeleport(teleportId);
    }

    public void keepAliveReceived(long id, long timestamp) {
        PingReplyResult result = keepAlives.receive(id, timestamp);
        this.observedKeepAliveReply = true;
        this.keepAlivePing = result.ping();
        this.lastKeepAliveReplyValid = result.valid();
        this.lastKeepAliveReplySkipped = result.skipped();
        this.lastSkippedKeepAliveReplyCount = result.skippedCount();
    }

    public void transactionReceived(int id, long timestamp) {
        PingReplyResult result = transactions.receive(id, timestamp);
        if (result.duplicate()) {
            return;
        }

        this.observedTransactionReply = true;
        this.transactionPing = result.ping();
        this.lastTransactionReplyValid = result.valid();
        this.lastTransactionReplySynthetic = result.synthetic();
        this.lastTransactionReplySkipped = result.skipped();
        this.lastSkippedTransactionReplyCount = result.skippedCount();
    }

    public void teleportReceived(int teleportId, long timestamp) {
        TeleportReplyResult result = transactions.receiveTeleport(teleportId, timestamp);
        this.observedTeleportReply = true;
        this.lastTeleportReplySkipped = result.skipped();
        this.lastSkippedTeleportReplyCount = result.skippedCount();
    }
}
