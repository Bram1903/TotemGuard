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

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.area.*;
import com.deathmotion.totemguard.common.physics.collision.SupportingBlockTracker;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSelect;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.rules.BounceRule;
import com.deathmotion.totemguard.common.physics.rules.CeilingFlushRule;
import com.deathmotion.totemguard.common.physics.rules.ClimbExitRule;
import com.deathmotion.totemguard.common.physics.rules.HoneySlideRule;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.util.ClientMath;

public final class AreaAdvance {

    private static final double GLIDE_PRESERVE_GAP = 0.1;
    private static final double VERTICAL_DUAL_GAP = 0.05;

    private final VersionGates gates;
    private final MediumSelect mediums;
    private final SupportingBlockTracker supportTracker;

    public AreaAdvance(VersionGates gates, MediumSelect mediums, SupportingBlockTracker supportTracker) {
        this.gates = gates;
        this.mediums = mediums;
        this.supportTracker = supportTracker;
    }

    public MotionArea advance(AreaBounds bounds, MotionArea area, JudgedExcess excess,
                              CarriedHypotheses.Kind kind, boolean carryPredicted,
                              TickState state, SpawnQueue queue) {
        MediumModel medium = state.medium;
        ControlEnvelope input = state.input;
        GroundFacts ground = state.ground;
        double frictionMax = state.frictionMax;
        double speedFactor = state.speedFactor;
        boolean airRegime = state.airRegime(kind);
        if (airRegime || (state.landModel() && bounds.pistonReached())) {
            frictionMax = LandModel.computeModifiedFriction(MotionDefaults.AIR_FRICTION, input.airDragModifier());
        }
        AreaAdvancer.clampObserved(bounds, state.dx, state.dy, state.dz, excess.altCenterUsed(),
                state.preset.modelDriftSlack());
        double anchor = (ground.groundedEnd() || state.stepFromFall) && !airRegime ? 0.0 : bounds.legalVy();
        double advancedVy = medium.advanceVertical(anchor, input);
        double advancedFloorVy = CeilingFlushRule.advanceFloor(anchor, advancedVy, medium, input, bounds, state.contact);
        advancedFloorVy = ClimbExitRule.carryFloor(advancedFloorVy, medium, mediums, bounds, input);
        boolean intervalOverridden = false;
        if (state.honeySlide) {
            double honeyVy = HoneySlideRule.carriedVy(gates.modernBlockEffects(), input.gravity());
            if (honeyVy > advancedVy) {
                advancedVy = honeyVy;
                intervalOverridden = true;
            }
        }
        if (input.powderSnowClimb()) {
            double climbVy = medium.advanceVertical(LandModel.POWDER_SNOW_CLIMB, input);
            if (climbVy > advancedVy) {
                advancedVy = climbVy;
                intervalOverridden = true;
            }
        }

        MotionArea next = AreaAdvancer.next(bounds.legalX(), bounds.legalZ(), frictionMax,
                speedFactor, advancedFloorVy, advancedVy);

        if (carryPredicted) {
            double accel = medium.accelBound(input, ground);
            double coastX = area.centerX();
            double coastZ = area.centerZ();
            double coastSlack = area.slack() + accel;
            if (input.sprintJump()) {
                coastX += input.boostX();
                coastZ += input.boostZ();
                coastSlack += input.boostSpread();
            }
            queue.queue(CarriedHypotheses.Kind.STEP_TRACK, TraceFrame.SPAWN_STEP,
                    new MotionArea(coastX * frictionMax, coastZ * frictionMax,
                            coastSlack * frictionMax, advancedFloorVy, advancedVy));
        }

        if (medium.kind() == MediumKind.GLIDE && mediums.glide().dualActive()) {
            double glideX = bounds.legalX() * speedFactor;
            double glideZ = bounds.legalZ() * speedFactor;
            double freeFallShrink = ClientMath.horizontalDistance(glideX, glideZ) * (1.0 - frictionMax);
            next = new MotionArea(glideX, glideZ, next.slack() + freeFallShrink,
                    mediums.land().advanceVertical(bounds.legalVy(), input), bounds.legalVy());
            intervalOverridden = true;
        }

        if (medium.kind() == MediumKind.GLIDE && state.contact.nearestSupportGap() <= GLIDE_PRESERVE_GAP) {
            double exitFloor = mediums.land().advanceVertical(bounds.floor(), input);
            if (exitFloor < next.floorVy()) {
                next = new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                        exitFloor, next.ceilVy());
                intervalOverridden = true;
            }
        }

        if (ground.bounced() && area.floorVy() < 0.0 && state.contact.supportBounce() > 0.0) {
            double reflected = BounceRule.reflectMax(gates.restitutionBounce(), state.contact,
                    area.floorVy(), input.gravity(), LandModel.verticalDrag(input));
            double advancedCeil = medium.advanceVertical(reflected, input);
            next = new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                    bounceFloor(advancedCeil, area, medium, input), advancedCeil);
            intervalOverridden = true;
        }

        next = AreaAdvancer.zeroClamp(next, gates.jointHorizontalZeroing());

        if (!intervalOverridden && next.ceilVy() - next.floorVy() > VERTICAL_DUAL_GAP) {
            queue.queue(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_VERTICAL_DUAL,
                    new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                            next.floorVy(), next.floorVy()));
            next = new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                    next.ceilVy(), next.ceilVy());
        }

        return next;
    }

    private double bounceFloor(double advancedCeil, MotionArea area, MediumModel medium, ControlEnvelope input) {
        if (!supportTracker.bounceCertain() || area.ceilVy() >= 0.0) return advancedCeil;
        double least = BounceRule.reflectMin(gates.restitutionBounce(), supportTracker.bounceFactor(),
                supportTracker.bounceBed(), area.ceilVy(), input.gravity());
        if (least <= 0.0) return medium.advanceVertical(0.0, input);
        return Math.min(advancedCeil, medium.advanceVertical(least, input));
    }
}
