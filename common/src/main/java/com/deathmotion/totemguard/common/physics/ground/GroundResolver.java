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

import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.collision.SupportingBlockTracker;
import com.deathmotion.totemguard.common.world.block.BlockTraits;
import org.jetbrains.annotations.Nullable;

public final class GroundResolver {

    private static final double GROUND_EPS = 0.02;
    private static final double RISE_EPS = 0.001;
    private static final double ARREST_EPS = 0.03;
    private static final double LANDING_EPS = 0.004;
    private static final double BOUNCE_MIN_DESCENT = 0.4;
    private static final int BOUNCE_UNCERTAINTY_TICKS = 3;

    private boolean lastGroundedEnd;
    private boolean prevGroundedEnd;
    private boolean lastFluid;
    private double lastGroundGap = Double.MAX_VALUE;
    private double prevObservedVy;
    private int bounceTicks;
    private int displacedTicks;
    private double lastSlipMin = BlockTraits.DEFAULT_SLIPPERINESS;
    private double lastSlipMax = BlockTraits.DEFAULT_SLIPPERINESS;
    private double prevSlipMin = BlockTraits.DEFAULT_SLIPPERINESS;
    private double prevSlipMax = BlockTraits.DEFAULT_SLIPPERINESS;
    private double lastJumpMin = 1.0;
    private double lastJumpMax = 1.0;

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

    public GroundFacts resolve(double observedVy, ContactReport contact, boolean fluidNow,
                               double stepHeight, double carriedFloor, boolean sneaking,
                               @Nullable SupportingBlockTracker support) {
        double gap = contact.nearestSupportGap();
        boolean wasFluid = lastFluid;
        lastFluid = fluidNow;

        boolean groundedStart = lastGroundedEnd;
        boolean supportedStart = groundedStart && lastGroundGap <= GROUND_EPS;
        boolean startAmbiguous = !groundedStart && (lastGroundGap <= GROUND_EPS || displacedTicks > 0);
        if (displacedTicks > 0) displacedTicks--;

        boolean rising = observedVy > RISE_EPS;
        boolean supportedNow = gap <= GROUND_EPS;
        boolean arrested = supportedNow && observedVy > carriedFloor + LANDING_EPS;
        boolean bounceContact = supportedNow && contact.supportBounce() > 0.0;
        boolean fellFreely = carriedFloor < 0.0 && observedVy <= carriedFloor + ARREST_EPS
                && !arrested && !bounceContact;
        boolean descendedLast = prevObservedVy < -RISE_EPS;
        boolean landingSupport = descendedLast && gap <= stepHeight;

        boolean groundedEnd;
        if (rising) {
            groundedEnd = supportedNow && (groundedStart || descendedLast);
        } else if (fellFreely) {
            groundedEnd = false;
        } else {
            groundedEnd = supportedNow || (groundedStart && lastGroundGap <= GROUND_EPS);
        }

        lastGroundGap = gap;
        prevObservedVy = observedVy;
        boolean recentlyGrounded = groundedStart || prevGroundedEnd || landingSupport;
        prevGroundedEnd = lastGroundedEnd;
        lastGroundedEnd = groundedEnd;

        boolean bounced = contact.supportBounce() > 0.0 && groundedEnd && !sneaking
                && carriedFloor < 0.0;
        if (bounced && carriedFloor < -BOUNCE_MIN_DESCENT) bounceTicks = BOUNCE_UNCERTAINTY_TICKS;
        else if (bounceTicks > 0) bounceTicks--;

        double startSlipMin;
        double startSlipMax;
        if (support != null && support.slipCertain()) {
            startSlipMin = support.slip();
            startSlipMax = support.slip();
        } else {
            startSlipMin = Math.min(lastSlipMin, prevSlipMin);
            startSlipMax = Math.max(lastSlipMax, prevSlipMax);
        }
        double startJumpMin;
        double startJumpMax;
        if (support != null && support.jumpCertain()) {
            startJumpMin = support.jumpFactor();
            startJumpMax = support.jumpFactor();
        } else {
            startJumpMin = lastJumpMin;
            startJumpMax = lastJumpMax;
        }
        prevSlipMin = lastSlipMin;
        lastSlipMin = contact.supportSlipMin();
        prevSlipMax = lastSlipMax;
        lastSlipMax = contact.supportSlipMax();
        lastJumpMin = contact.supportJumpMin();
        lastJumpMax = contact.supportJumpMax();

        GroundState start = groundedStart ? GroundState.SUPPORTED
                : startAmbiguous ? GroundState.AMBIGUOUS
                  : GroundState.AIRBORNE;
        return new GroundFacts(start, groundedEnd, supportedStart, arrested, recentlyGrounded, landingSupport, bounced,
                bounceTicks > 0, wasFluid, startSlipMin, startSlipMax, startJumpMin, startJumpMax, gap);
    }

    public boolean lastGroundedEnd() {
        return lastGroundedEnd;
    }

    public void clearWindows() {
        bounceTicks = 0;
        displacedTicks = 0;
    }

    public void reset() {
        clearWindows();
        lastSlipMin = BlockTraits.DEFAULT_SLIPPERINESS;
        lastSlipMax = BlockTraits.DEFAULT_SLIPPERINESS;
        prevSlipMin = BlockTraits.DEFAULT_SLIPPERINESS;
        prevSlipMax = BlockTraits.DEFAULT_SLIPPERINESS;
        lastJumpMin = 1.0;
        lastJumpMax = 1.0;
    }
}
