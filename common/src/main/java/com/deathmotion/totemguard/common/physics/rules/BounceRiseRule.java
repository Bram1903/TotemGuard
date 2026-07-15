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

package com.deathmotion.totemguard.common.physics.rules;

import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;

public final class BounceRiseRule {

    private boolean armed;
    private double rise;
    private double headroom;

    public void arm(boolean landModel, boolean doubleMove, boolean fullPose, boolean sneaking,
                    boolean stuck, boolean honeySlide, boolean bounceCertain,
                    double observedDy, double carriedCeilBefore, double carriedRise, double headroom) {
        armed = landModel && !doubleMove && fullPose && !sneaking && !stuck && !honeySlide
                && bounceCertain
                && carriedCeilBefore < 0.0
                && observedDy <= 0.0 && observedDy > carriedCeilBefore
                && carriedRise > 0.0;
        rise = carriedRise;
        this.headroom = headroom;
    }

    public void disarm() {
        armed = false;
    }

    public double required(boolean tainted, ControlEnvelope input, double jumpTakeoffMin) {
        if (!armed || tainted) return 0.0;
        double owed = Math.min(rise, headroom);
        if (input.jumpPossible()) owed = Math.min(owed, jumpTakeoffMin);
        return owed;
    }
}
