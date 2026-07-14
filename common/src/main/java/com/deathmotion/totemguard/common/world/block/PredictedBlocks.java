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

package com.deathmotion.totemguard.common.world.block;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PredictedBlocks {

    public static final int NONE = -1;
    private static final int NO_SEQUENCE = 0;
    private static final int MAX_PREDICTIONS = 32;

    private static final int DENIAL_LIMIT = 3;
    private static final long DENIAL_WINDOW_MILLIS = 3000;
    private static final long DENIAL_COOLDOWN_MILLIS = 2500;

    private final Map<Long, Prediction> predictions = new ConcurrentHashMap<>();
    private final Map<Long, Denial> denials = new ConcurrentHashMap<>();

    public boolean predict(int x, int y, int z, int serverStateId, int sequence, int chainDepth, long nowMillis) {
        long key = PendingBlocks.blockKey(x, y, z);
        if (lockedOut(key, nowMillis)) return false;
        if (predictions.size() >= MAX_PREDICTIONS && !predictions.containsKey(key)) {
            return false;
        }
        predictions.put(key, new Prediction(serverStateId, sequence, chainDepth, nowMillis));
        return true;
    }

    public void recordDenial(int x, int y, int z, long nowMillis) {
        long key = PendingBlocks.blockKey(x, y, z);
        Denial denial = denials.get(key);
        if (denial == null || nowMillis - denial.windowStartMillis > DENIAL_WINDOW_MILLIS) {
            denials.put(key, new Denial(1, nowMillis));
        } else {
            denials.put(key, new Denial(denial.count + 1, denial.windowStartMillis));
        }
    }

    private boolean lockedOut(long key, long nowMillis) {
        Denial denial = denials.get(key);
        if (denial == null) return false;
        if (denial.count < DENIAL_LIMIT) return false;
        return nowMillis - denial.windowStartMillis <= DENIAL_WINDOW_MILLIS + DENIAL_COOLDOWN_MILLIS;
    }

    public int peek(int x, int y, int z) {
        Prediction prediction = predictions.get(PendingBlocks.blockKey(x, y, z));
        return prediction == null ? NONE : prediction.serverStateId;
    }

    public boolean has(int x, int y, int z) {
        return !predictions.isEmpty() && predictions.containsKey(PendingBlocks.blockKey(x, y, z));
    }

    public int chainDepth(int x, int y, int z) {
        Prediction prediction = predictions.get(PendingBlocks.blockKey(x, y, z));
        return prediction == null ? 0 : prediction.chainDepth;
    }

    public void drop(int x, int y, int z) {
        predictions.remove(PendingBlocks.blockKey(x, y, z));
    }

    public void dropUpToSequence(int sequence) {
        if (predictions.isEmpty()) return;
        predictions.values().removeIf(prediction -> prediction.sequence != NO_SEQUENCE && prediction.sequence <= sequence);
    }

    public void expire(long nowMillis, long timeoutMillis) {
        if (!predictions.isEmpty()) {
            Iterator<Prediction> iterator = predictions.values().iterator();
            while (iterator.hasNext()) {
                if (nowMillis - iterator.next().createdMillis > timeoutMillis) iterator.remove();
            }
        }
        if (!denials.isEmpty()) {
            denials.values().removeIf(denial ->
                    nowMillis - denial.windowStartMillis > DENIAL_WINDOW_MILLIS + DENIAL_COOLDOWN_MILLIS);
        }
    }

    public int size() {
        return predictions.size();
    }

    public void clear() {
        predictions.clear();
        denials.clear();
    }

    private record Prediction(int serverStateId, int sequence, int chainDepth, long createdMillis) {
    }

    private record Denial(int count, long windowStartMillis) {
    }
}
