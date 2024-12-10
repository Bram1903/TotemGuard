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

package com.deathmotion.totemguard.api.interfaces;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public interface ITotemData {
    /**
     * Gets the collection of intervals.
     *
     * @return the intervals as a ConcurrentLinkedDeque
     */
    ConcurrentLinkedDeque<Long> getIntervals();

    /**
     * Gets the latest standard deviation.
     *
     * @return the latest standard deviation
     */
    double getLatestStandardDeviation();

    /**
     * Retrieves the latest intervals up to the specified amount.
     *
     * @param amount the number of latest intervals to retrieve
     * @return a list of the latest intervals
     */
    List<Long> getLatestIntervals(int amount);
}
