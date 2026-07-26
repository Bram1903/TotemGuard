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

package com.deathmotion.totemguard.common.replay.playback;

import com.deathmotion.totemguard.common.replay.format.RecordingHeader;
import com.deathmotion.totemguard.common.replay.format.TickDigest;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record ReplayResult(
        RecordingHeader header,
        boolean truncated,
        long frames,
        long ticks,
        long judged,
        long coasted,
        long declined,
        Map<String, Long> declineReasons,
        List<ReplayFlag> flags,
        FlagCheck flagCheck,
        List<TickDigest> digests,
        Verification verification,
        WorldCheck worldCheck,
        Map<String, Integer> selfEmitted,
        Map<String, Integer> dispatchErrors,
        int pendingEventLoopTasks,
        long elapsedMillis,
        @Nullable String error
) {
    public static final int WARM_UP_TICKS = 40;

    public boolean vacuous() {
        return judged == 0;
    }

    public boolean prologued() {
        return worldCheck.status() != WorldCheck.Status.ABSENT;
    }

    public long warmUpUntilTick() {
        if (!prologued() || digests.isEmpty()) return Long.MIN_VALUE;
        return digests.get(0).tick() + WARM_UP_TICKS;
    }

    public List<ReplayFlag> settledFlags() {
        long until = warmUpUntilTick();
        if (until == Long.MIN_VALUE) return flags;
        return flags.stream().filter(flag -> flag.tick() > until).toList();
    }

    public int warmUpFlags() {
        return flags.size() - settledFlags().size();
    }

    public record FlagCheck(Status status, int recorded, int replayed, @Nullable String difference) {

        public static final FlagCheck ABSENT = new FlagCheck(Status.ABSENT, 0, 0, null);

        public static FlagCheck matched(int count) {
            return new FlagCheck(Status.MATCHED, count, count, null);
        }

        public static FlagCheck diverged(int recorded, int replayed, String difference) {
            return new FlagCheck(Status.DIVERGED, recorded, replayed, difference);
        }

        public enum Status {

            ABSENT,
            MATCHED,
            DIVERGED
        }
    }

    public record WorldCheck(Status status, int columnsLoaded, @Nullable String difference) {

        public static final WorldCheck ABSENT = new WorldCheck(Status.ABSENT, 0, null);

        public static WorldCheck matched(int columnsLoaded) {
            return new WorldCheck(Status.MATCHED, columnsLoaded, null);
        }

        public static WorldCheck diverged(int columnsLoaded, String difference) {
            return new WorldCheck(Status.DIVERGED, columnsLoaded, difference);
        }

        public static WorldCheck unsettled() {
            return new WorldCheck(Status.UNSETTLED, 0, "the world never settled before the recording ended");
        }

        public static WorldCheck vacuous() {
            return new WorldCheck(Status.VACUOUS, 0,
                    "no column was loaded where the digest sampled, so nothing was compared");
        }

        public enum Status {

            ABSENT,
            MATCHED,
            DIVERGED,
            UNSETTLED,
            VACUOUS
        }
    }

    public record Verification(Status status, long compared, long total, long divergentTick,
                               @Nullable String field, @Nullable String skipReason) {
        public static Verification skipped(String reason) {
            return new Verification(Status.SKIPPED, 0, 0, -1, null, reason);
        }

        public static Verification matched(long compared) {
            return new Verification(Status.MATCHED, compared, compared, -1, null, null);
        }

        public static Verification diverged(long compared, long total, long tick, String field) {
            return new Verification(Status.DIVERGED, compared, total, tick, field, null);
        }

        public enum Status {

            MATCHED,
            DIVERGED,
            SKIPPED
        }
    }
}
