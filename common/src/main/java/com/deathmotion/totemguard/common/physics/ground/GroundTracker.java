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

package com.deathmotion.totemguard.common.physics.ground;

import com.deathmotion.totemguard.common.physics.MovementConstants;
import com.deathmotion.totemguard.common.world.scan.BlockEnvironment;

public final class GroundTracker {

    private static final double GROUND_EPS = 0.02;
    private static final double GROUND_RISE_EPS = 0.001;
    private static final double GROUND_ARREST_EPS = 0.03;
    private static final double GROUND_LANDING_EPS = 0.004;
    private static final double BOUNCE_MIN_DESCENT = 0.4;
    private static final int BOUNCE_UNCERTAINTY_TICKS = 3;

    private boolean lastGroundedEnd;
    private boolean prevGroundedEnd;
    private boolean lastFluid;
    private double lastGroundGap;
    private double prevObservedVy;
    private int bounceTicks;
    private int displacedTicks;
    private double lastSlipperinessMin = MovementConstants.DEFAULT_SLIPPERINESS;
    private double lastSlipperinessMax = MovementConstants.DEFAULT_SLIPPERINESS;

    public void seed(boolean onGround) {
        lastGroundedEnd = onGround;
        prevGroundedEnd = onGround;
        lastFluid = false;
        lastGroundGap = onGround ? 0.0 : Double.MAX_VALUE;
        prevObservedVy = 0.0;
        displacedTicks = 0;
    }

    public void displaced() {
        lastGroundedEnd = false;
        prevGroundedEnd = false;
        lastGroundGap = Double.MAX_VALUE;
        prevObservedVy = 0.0;
        displacedTicks = 1;
    }

    public GroundState resolve(double observedVy, BlockEnvironment env, double stepHeight, double carriedFloor, boolean sneaking) {
        boolean wasFluid = lastFluid;
        lastFluid = env.fluid();

        boolean groundedStart = lastGroundedEnd;
        boolean startAmbiguous = !groundedStart && (lastGroundGap <= GROUND_EPS || displacedTicks > 0);
        if (displacedTicks > 0) displacedTicks--;
        boolean rising = observedVy > GROUND_RISE_EPS;
        boolean supportedNow = env.groundGap() <= GROUND_EPS;
        boolean arrested = supportedNow && observedVy > carriedFloor + GROUND_LANDING_EPS;
        boolean bounceContact = supportedNow && env.bounceFactor() > 0.0;
        boolean fellFreely = carriedFloor < 0.0 && observedVy <= carriedFloor + GROUND_ARREST_EPS
                && !arrested && !bounceContact;
        boolean descendedLast = prevObservedVy < -GROUND_RISE_EPS;
        boolean landingSupport = descendedLast && env.groundGap() <= stepHeight;
        boolean groundedEnd;
        if (rising) {
            groundedEnd = supportedNow && (groundedStart || descendedLast);
        } else if (fellFreely) {
            groundedEnd = false;
        } else {
            groundedEnd = supportedNow || (groundedStart && lastGroundGap <= GROUND_EPS);
        }
        lastGroundGap = env.groundGap();
        prevObservedVy = observedVy;

        boolean recentlyGrounded = groundedStart || prevGroundedEnd || landingSupport;
        prevGroundedEnd = lastGroundedEnd;
        lastGroundedEnd = groundedEnd;

        boolean bounced = env.bounceFactor() > 0.0 && groundedEnd && !sneaking && carriedFloor < -BOUNCE_MIN_DESCENT;
        if (bounced) bounceTicks = BOUNCE_UNCERTAINTY_TICKS;
        else if (bounceTicks > 0) bounceTicks--;

        double startSlipMin = lastSlipperinessMin;
        double startSlipMax = lastSlipperinessMax;
        lastSlipperinessMin = env.slipperinessMin();
        lastSlipperinessMax = env.slipperinessMax();

        return new GroundState(groundedStart, startAmbiguous, groundedEnd, recentlyGrounded, landingSupport, bounced,
                carriedFloor, bounceTicks > 0, wasFluid, startSlipMin, startSlipMax);
    }

    public void clearWindows() {
        bounceTicks = 0;
        displacedTicks = 0;
    }

    public void reset() {
        clearWindows();
        lastSlipperinessMin = MovementConstants.DEFAULT_SLIPPERINESS;
        lastSlipperinessMax = MovementConstants.DEFAULT_SLIPPERINESS;
    }
}
