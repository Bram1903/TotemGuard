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

public class ExternalVelocityData {

    private static final int WINDOW_TICKS = 3;

    private double x;
    private double y;
    private double z;
    private int ticks;
    private boolean active;

    public void addPush(double px, double py, double pz) {
        if (active) {
            x += px;
            y += py;
            z += pz;
        } else {
            x = px;
            y = py;
            z = pz;
        }
        active = true;
        ticks = WINDOW_TICKS;
    }

    public void tick() {
        if (!active) return;
        if (--ticks <= 0) clear();
    }

    public void consume() {
        clear();
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public boolean isActive() {
        return active;
    }

    public void reset() {
        clear();
    }

    private void clear() {
        x = 0.0;
        y = 0.0;
        z = 0.0;
        ticks = 0;
        active = false;
    }
}
