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

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;

public final class RiseObligation {

    private final BounceRiseRule bounce = new BounceRiseRule();
    private final LevitationRiseRule levitation = new LevitationRiseRule();

    private double bounceOwed;
    private boolean levitationArmed;
    private double headroom;

    public void prepare(boolean tainted, ControlEnvelope input, double jumpTakeoffMin,
                        MediumSample sample, ContactReport contact) {
        levitation.observe(input);
        bounceOwed = bounce.required(tainted, input, jumpTakeoffMin);
        boolean levitationTainted = tainted
                || sample.climbable() || sample.climbableUncertain()
                || contact.startOverlapping() || contact.supportIsEntity();
        levitationArmed = levitation.armed(levitationTainted, input);
        headroom = contact.ceilingClearanceAny();
    }

    public double riseFloorFor(AreaBounds slotBounds) {
        double owed = bounceOwed;
        if (levitationArmed && slotBounds.floor() > 0.0) {
            owed = Math.max(owed, Math.min(slotBounds.floor(), headroom));
        }
        return owed;
    }

    public boolean bouncePinned() {
        return bounceOwed > 0.0;
    }

    public BoundBreach breachLabel() {
        return bounceOwed > 0.0 ? BoundBreach.BOUNCE_RISE : BoundBreach.FORCED_RISE;
    }

    public void armBounce(boolean landModel, boolean doubleMove, boolean fullPose, boolean sneaking,
                          boolean stuck, boolean honeySlide, boolean bounceCertain,
                          double observedDy, double carriedCeilBefore, double carriedRise,
                          double ceilingClearance) {
        bounce.arm(landModel, doubleMove, fullPose, sneaking, stuck, honeySlide, bounceCertain,
                observedDy, carriedCeilBefore, carriedRise, ceilingClearance);
    }

    public void disarmBounce() {
        bounce.disarm();
        bounceOwed = 0.0;
        levitationArmed = false;
    }

    public void disarm() {
        disarmBounce();
        levitation.reset();
    }
}
