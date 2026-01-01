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

package com.deathmotion.totemguard.common.check;

public final class Buffer {

    private double value;

    private static double clamp(double value) {
        if (value > Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        } else if (value < -Double.MAX_VALUE) {
            return -Double.MAX_VALUE;
        }

        return value;
    }

    public double get() {
        return value;
    }

    public double increase() {
        return increase(1.0);
    }

    public double increase(double amount) {
        value = clamp(value + amount);
        return value;
    }

    public double decrease() {
        return decrease(1.0);
    }

    public double decrease(double amount) {
        value = clamp(value - amount);
        return value;
    }

    public void reset() {
        value = 0.0;
    }

    public void set(double amount) {
        value = clamp(amount);
    }

    public void multiply(double multiplier) {
        value = clamp(value * multiplier);
    }
}

