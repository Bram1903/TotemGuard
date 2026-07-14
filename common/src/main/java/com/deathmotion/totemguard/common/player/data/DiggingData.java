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

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

@Accessors(fluent = true)
public final class DiggingData {

    public static final double SERVER_ACCEPT_FRACTION = 0.7;
    private static final long EXPECTED_CAP_MILLIS = 10L * 60L * 1000L;
    private static final int EXPECTED_CAP_TICKS = 10 * 60 * 20;
    private static final long MIRROR_EDIT_JITTER_MILLIS = 120;
    private static final int START_WINDOW_SIZE = 48;
    private static final long START_WINDOW_MILLIS = 1000;

    private final long[] startTimestamps = new long[START_WINDOW_SIZE];
    private int startRingIndex;

    private boolean targetValid;
    @Getter
    private int targetX;
    @Getter
    private int targetY;
    @Getter
    private int targetZ;
    private ItemType targetItem;
    private long anchorMillis;
    private float bestPerTickProgress;

    private long lastFinishMillis;

    private FinishJudgment finishJudgment;
    private boolean wrongFinish;
    private boolean wrongAbort;
    @Getter
    private int startsInWindow;

    private static int expectedTicks(float perTickProgress) {
        if (perTickProgress <= 0.0f) return EXPECTED_CAP_TICKS;
        if (perTickProgress >= 1.0f) return 1;
        float accumulated = 0.0f;
        int ticks = 0;
        while (accumulated < 1.0f && ticks < EXPECTED_CAP_TICKS) {
            accumulated += perTickProgress;
            ticks++;
        }
        return ticks;
    }

    private static long expectedMillis(float perTickProgress) {
        return expectedTicks(perTickProgress) * 50L;
    }

    public void onStart(boolean instant, int x, int y, int z, ItemType item, float perTickProgress, long nowMillis) {
        startTimestamps[startRingIndex] = nowMillis;
        startRingIndex = (startRingIndex + 1) % START_WINDOW_SIZE;
        int count = 0;
        for (long timestamp : startTimestamps) {
            if (timestamp != 0 && nowMillis - timestamp <= START_WINDOW_MILLIS) count++;
        }
        startsInWindow = count;

        if (instant) return;

        targetValid = true;
        targetX = x;
        targetY = y;
        targetZ = z;
        targetItem = item;
        anchorMillis = nowMillis;
        bestPerTickProgress = perTickProgress;
    }

    public void onAbort(int x, int y, int z, long nowMillis) {
        if (!targetValid) return;
        if (x == targetX && y == targetY && z == targetZ) {
            anchorMillis = Math.max(anchorMillis, nowMillis);
        } else {
            wrongAbort = true;
        }
    }

    public boolean onFinish(int x, int y, int z, long nowMillis) {
        if (!targetValid || x != targetX || y != targetY || z != targetZ) {
            wrongFinish = true;
            return false;
        }

        long expected = expectedMillis(bestPerTickProgress);
        long elapsed = Math.max(0, nowMillis - anchorMillis);
        long sinceLastFinish = lastFinishMillis == 0 ? Long.MAX_VALUE : Math.max(0, nowMillis - lastFinishMillis);
        long requiredGap = Math.min(EXPECTED_CAP_MILLIS, expected + 5 * 50L);
        boolean mirrorEditable = elapsed + MIRROR_EDIT_JITTER_MILLIS >= (long) (expected * SERVER_ACCEPT_FRACTION);

        finishJudgment = new FinishJudgment(expected, elapsed, sinceLastFinish, requiredGap, mirrorEditable);
        lastFinishMillis = nowMillis;
        anchorMillis = nowMillis;
        return mirrorEditable;
    }

    public void sample(ItemType currentItem, float perTickProgress) {
        if (!targetValid || currentItem != targetItem) return;
        if (perTickProgress > bestPerTickProgress) bestPerTickProgress = perTickProgress;
    }

    public boolean hasTarget() {
        return targetValid;
    }

    public FinishJudgment pollFinishJudgment() {
        FinishJudgment judgment = finishJudgment;
        finishJudgment = null;
        return judgment;
    }

    public boolean pollWrongFinish() {
        boolean value = wrongFinish;
        wrongFinish = false;
        return value;
    }

    public boolean pollWrongAbort() {
        boolean value = wrongAbort;
        wrongAbort = false;
        return value;
    }

    public void reset() {
        targetValid = false;
        targetItem = null;
        anchorMillis = 0;
        bestPerTickProgress = 0;
        lastFinishMillis = 0;
        finishJudgment = null;
        wrongFinish = false;
        wrongAbort = false;
        startsInWindow = 0;
        startRingIndex = 0;
        Arrays.fill(startTimestamps, 0L);
    }

    public record FinishJudgment(long expectedMillis, long elapsedMillis,
                                 long sinceLastFinishMillis, long requiredGapMillis,
                                 boolean mirrorEditable) {
    }
}
