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

import com.deathmotion.totemguard.common.TGPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.logging.Level;

final class PendingTransaction {

    private final int id;
    private final List<LongConsumer> callbacks = new ArrayList<>();
    private Long sentAt;
    private boolean synthetic;

    PendingTransaction(int id) {
        this.id = id;
    }

    int id() {
        return id;
    }

    Long sentAt() {
        return sentAt;
    }

    boolean synthetic() {
        return synthetic;
    }

    void addCallback(LongConsumer callback) {
        callbacks.add(callback);
    }

    void setSentAt(long timestamp) {
        this.sentAt = timestamp;
    }

    void setSynthetic(boolean synthetic) {
        this.synthetic = synthetic;
    }

    void runCallbacks(long timestamp) {
        for (LongConsumer callback : callbacks) {
            try {
                callback.accept(timestamp);
            } catch (Exception exception) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to execute transaction callback.", exception);
            }
        }
    }
}
