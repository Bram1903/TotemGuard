/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.util;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Utility class that provides mathematical functions for working with totem event intervals.
 */
public class MathUtil {

    /**
     * Calculates the intervals between totem use events and re-equip events.
     * <p>
     * This method iterates through two {@link ConcurrentLinkedDeque}s in parallel and computes the time
     * differences between corresponding use and re-equip events. The returned array contains the time intervals
     * for each matched pair.
     *
     * @param useTimes     A deque containing timestamps (in nanoseconds) of totem use events.
     * @param reEquipTimes A deque containing timestamps (in nanoseconds) of totem re-equip events.
     * @return A long array containing the time intervals between corresponding totem use and re-equip events.
     * The size of the array is the minimum of the sizes of the two-input dequeue.
     */
    public static long[] calculateIntervals(ConcurrentLinkedDeque<Long> useTimes, ConcurrentLinkedDeque<Long> reEquipTimes) {
        int size = Math.min(useTimes.size(), reEquipTimes.size());
        long[] intervals = new long[size];

        int i = 0;
        var useIter = useTimes.iterator();
        var reEquipIter = reEquipTimes.iterator();

        // Iterate through both dequeue in parallel
        while (useIter.hasNext() && reEquipIter.hasNext()) {
            Long useTime = useIter.next();
            Long reEquipTime = reEquipIter.next();

            if (useTime != null && reEquipTime != null) {
                intervals[i++] = reEquipTime - useTime;  // Calculate time difference between use and re-equip
            }
        }

        return intervals;
    }
}
