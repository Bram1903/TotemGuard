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

import java.util.function.LongConsumer;

public class PingData {

    static final int INVALID_PING = -1;

    private final PendingReplies replies = new PendingReplies();

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
    private boolean lastTeleportReplyValid;
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
        replies.sendKeepAlive(id, timestamp);
    }

    public int reserveNextTransactionId(int maxPositiveId) {
        return replies.reserveTransactionId(maxPositiveId);
    }

    public void addTransactionCallback(int id, LongConsumer callback) {
        replies.addTransactionCallback(id, callback);
    }

    public void transactionSent(int id, long timestamp) {
        replies.sendTransaction(id, timestamp);
    }

    public void markTransactionSynthetic(int id) {
        replies.markTransactionSynthetic(id);
    }

    public boolean shouldCancelTransactionReplyOnProxy() {
        return lastTransactionReplySynthetic;
    }

    public int getPendingTransactionCount() {
        return replies.pendingTransactions();
    }

    public int getPendingSyntheticTransactionCount() {
        return replies.pendingSyntheticTransactions();
    }

    public int getAcceptedTransactionCount() {
        return replies.acceptedTransactions();
    }

    public int getAcceptedSyntheticTransactionCount() {
        return replies.acceptedSyntheticTransactions();
    }

    public void teleportSent(int teleportId, long timestamp) {
        replies.sendTeleport(teleportId, timestamp);
    }

    public void keepAliveReceived(long id, long timestamp) {
        ReplyResult result = replies.receive(ReplyType.KEEP_ALIVE, id, timestamp);
        this.observedKeepAliveReply = true;
        this.keepAlivePing = result.ping();
        this.lastKeepAliveReplyValid = result.valid();
        this.lastKeepAliveReplySkipped = result.skipped();
        this.lastSkippedKeepAliveReplyCount = result.skippedCount();
    }

    public void transactionReceived(int id, long timestamp) {
        ReplyResult result = replies.receive(ReplyType.TRANSACTION, id, timestamp);
        this.observedTransactionReply = true;
        this.transactionPing = result.ping();
        this.lastTransactionReplyValid = result.valid();
        this.lastTransactionReplySynthetic = result.synthetic();
        this.lastTransactionReplySkipped = result.skipped();
        this.lastSkippedTransactionReplyCount = result.skippedCount();
    }

    public void teleportReceived(int teleportId, long timestamp) {
        ReplyResult result = replies.receive(ReplyType.TELEPORT, teleportId, timestamp);
        this.observedTeleportReply = true;
        this.lastTeleportReplyValid = result.valid();
        this.lastTeleportReplySkipped = result.skipped();
        this.lastSkippedTeleportReplyCount = result.skippedCount();
    }
}
