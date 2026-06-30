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

package com.deathmotion.totemguard.common.player.movement.area;

public record MotionArea(Range horizontalSpeed, Range vertical) {

    public static MotionArea resting() {
        return new MotionArea(Range.ZERO, Range.ZERO);
    }

    public static MotionArea of(double horizontalSpeed, double verticalMotion) {
        return new MotionArea(Range.of(horizontalSpeed), Range.of(verticalMotion));
    }

    public double horizontalExcess(double observedSpeed) {
        return horizontalSpeed.excessAbove(observedSpeed);
    }

    public double ascentExcess(double observedVerticalMotion) {
        return vertical.excessAbove(observedVerticalMotion);
    }

    public MotionArea expand(double horizontal, double ascent) {
        return new MotionArea(horizontalSpeed.grow(0.0, horizontal), vertical.grow(0.0, ascent));
    }
}
