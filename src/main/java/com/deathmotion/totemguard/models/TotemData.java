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

package com.deathmotion.totemguard.models;

import com.deathmotion.totemguard.util.MathUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Getter
public class TotemData {
    @Setter
    private boolean expectingTotemSwap;

    @Setter
    private Long lastTotemUsage;

    @Getter
    private final ConcurrentLinkedDeque<Long> intervals = new ConcurrentLinkedDeque<>();

    @Setter
    private double latestStandardDeviation;

    public void addInterval(long interval) {
        intervals.addLast(interval);

        if (intervals.size() >= 50) {
            intervals.poll();
        }

        latestStandardDeviation = MathUtil.trim(2, MathUtil.getStandardDeviation(intervals));
    }

    public List<Long> getLatestIntervals(int amount) {
        return intervals.stream()
                .skip(Math.max(0, intervals.size() - amount))
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.reverse(list);
                    return list;
                }));
    }
}
