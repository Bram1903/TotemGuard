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

package com.deathmotion.totemguard.common.physics.simulation;

import com.deathmotion.totemguard.common.physics.area.CarriedHypotheses;
import com.deathmotion.totemguard.common.physics.area.MotionArea;

public final class SpawnQueue {

    private static final int LIMIT = 12;

    private final MotionArea[] areas = new MotionArea[LIMIT];
    private final CarriedHypotheses.Kind[] kinds = new CarriedHypotheses.Kind[LIMIT];
    private int count;
    private long bits;

    public void begin() {
        count = 0;
        bits = 0L;
    }

    public void queue(CarriedHypotheses.Kind kind, long bit, MotionArea area) {
        if (count >= LIMIT) return;
        kinds[count] = kind;
        areas[count] = area;
        count++;
        bits |= bit;
    }

    public void spawnNow(CarriedHypotheses carried, CarriedHypotheses.Kind kind, long bit, MotionArea area) {
        carried.spawn(kind, area);
        bits |= bit;
    }

    public void flush(CarriedHypotheses carried) {
        for (int i = 0; i < count; i++) {
            carried.spawn(kinds[i], areas[i]);
            areas[i] = null;
        }
        count = 0;
    }

    public void drop() {
        count = 0;
    }

    public long bits() {
        return bits;
    }
}
