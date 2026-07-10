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

package com.deathmotion.totemguard.common.physics.medium.model;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.player.data.FireworkData;
import com.deathmotion.totemguard.common.util.ClientMath;

public final class GlideModel implements MediumModel {

    private static final double LIFT_FACTOR = 0.75;
    private static final double SINK_TO_FORWARD = 0.1;
    private static final double PITCH_UP_COST = 0.04;
    private static final double PITCH_UP_LIFT = 3.2;
    private static final double STEER_RATE = 0.1;
    private static final double DRAG_H = 0.99;
    private static final double DRAG_V = 0.98;
    private static final double SLOW_FALLING_GRAVITY = 0.01;
    private static final double BOOST_KEEP = 0.5;
    private static final double BOOST_ADD = 0.85;
    private static final double TRIG_PAD = 0.003;

    private final LandModel land;

    private boolean dual;
    private boolean coasting;
    private int boostMin;
    private int boostMax;

    private double stepX;
    private double stepZ;
    private double stepFloor;
    private double stepCeil;
    private double stepRadius;
    private double coastAccel;

    public GlideModel(LandModel land) {
        this.land = land;
    }

    @Override
    public MediumKind kind() {
        return MediumKind.GLIDE;
    }

    public void prepare(boolean dual, boolean coasting, FireworkData fireworks) {
        this.dual = dual;
        this.coasting = coasting;
        this.boostMin = fireworks.boostCountMin();
        this.boostMax = fireworks.boostCountMax();
    }

    @Override
    public double accelBound(ControlEnvelope input, GroundFacts ground) {
        return coastAccel;
    }

    @Override
    public void horizontalOptions(ControlEnvelope input, GroundFacts ground, AreaBounds bounds) {
        computeStep(input, bounds);
        if (dual) {
            land.horizontalOptions(input, ground, bounds);
            double dx = stepX - bounds.centerX();
            double dz = stepZ - bounds.centerZ();
            bounds.expandRadius(Math.sqrt(dx * dx + dz * dz));
            bounds.expandRadius(stepRadius + TRIG_PAD);
            return;
        }
        bounds.centerX(stepX);
        bounds.centerZ(stepZ);
        bounds.expandRadius(stepRadius + TRIG_PAD);
        if (input.jumpPossible() && input.sprinting()) bounds.expandRadius(MediumModel.SPRINT_JUMP_BOOST);
        bounds.expandRadius(input.sprintJumpResidual());
    }

    @Override
    public void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        if (dual) {
            land.verticalOptions(input, ground, contact, bounds);
            bounds.ceiling(Math.max(bounds.ceiling(), stepCeil + TRIG_PAD));
            bounds.lowerFloor(stepFloor - TRIG_PAD);
            bounds.enforceDescentFloor(true);
            return;
        }
        bounds.ceiling(stepCeil + TRIG_PAD);
        bounds.floor(stepFloor - TRIG_PAD);
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff());
        if (ground.groundedEnd()) bounds.raiseCeiling(0.0);
        bounds.enforceDescentFloor(true);
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return dual ? land.frictionMax(input, ground) : 1.0;
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        return dual ? land.advanceVertical(verticalVelocity, input) : verticalVelocity;
    }

    public boolean dualActive() {
        return dual;
    }

    public void reset() {
        dual = false;
        coasting = false;
        boostMin = 0;
        boostMax = 0;
        coastAccel = 0.0;
    }

    private void computeStep(ControlEnvelope input, AreaBounds bounds) {
        double vx = bounds.centerX();
        double vz = bounds.centerZ();
        double floor = bounds.floor();
        double ceil = bounds.ceiling();

        if (coasting) {
            double speed = ClientMath.horizontalDistance(vx, vz);
            double sinkGain = Math.max(0.0, -floor) * SINK_TO_FORWARD;
            double boostGain = boostMax > 0
                    ? boostMax * (BOOST_ADD + BOOST_KEEP * (speed + Math.max(Math.abs(floor), Math.abs(ceil))))
                    : 0.0;
            stepX = vx;
            stepZ = vz;
            stepRadius = 0.0;
            coastAccel = 2.0 * STEER_RATE * speed + sinkGain + boostGain + TRIG_PAD;
            stepCeil = (ceil + speed * PITCH_UP_COST * PITCH_UP_LIFT + boostGain) * DRAG_V;
            stepFloor = (floor - input.gravity()) * DRAG_V - boostGain;
            return;
        }
        coastAccel = 0.0;

        double curX = input.lookX(), curY = input.lookY(), curZ = input.lookZ();
        double curD0 = input.lookHorizontal();
        double prvX = input.prevLookX(), prvY = input.prevLookY(), prvZ = input.prevLookZ();
        double prvD0 = ClientMath.horizontalDistance(prvX, prvZ);

        double hiX = vx, hiZ = vz, hiFloor = floor, hiCeil = ceil;
        for (int i = 0; i < boostMax; i++) {
            hiX = BOOST_KEEP * hiX + BOOST_ADD * curX;
            hiZ = BOOST_KEEP * hiZ + BOOST_ADD * curZ;
            hiFloor = BOOST_KEEP * hiFloor + BOOST_ADD * curY;
            hiCeil = BOOST_KEEP * hiCeil + BOOST_ADD * curY;
        }
        double hiVy = Math.min(hiFloor, hiCeil);
        double hiOutX = stepHorizontal(hiX, curX, hiX, hiZ, hiVy, curY, curD0, input);
        double hiOutZ = stepHorizontal(hiZ, curZ, hiX, hiZ, hiVy, curY, curD0, input);

        double loX = vx, loZ = vz, loFloor = floor, loCeil = ceil;
        for (int i = 0; i < boostMin; i++) {
            loX = BOOST_KEEP * loX + BOOST_ADD * curX;
            loZ = BOOST_KEEP * loZ + BOOST_ADD * curZ;
            loFloor = BOOST_KEEP * loFloor + BOOST_ADD * curY;
            loCeil = BOOST_KEEP * loCeil + BOOST_ADD * curY;
        }
        double loVy = Math.min(loFloor, loCeil);
        double loOutX = stepHorizontal(loX, curX, loX, loZ, loVy, curY, curD0, input);
        double loOutZ = stepHorizontal(loZ, curZ, loX, loZ, loVy, curY, curD0, input);

        double pvX = vx, pvZ = vz, pvFloor = floor, pvCeil = ceil;
        for (int i = 0; i < boostMax; i++) {
            pvX = BOOST_KEEP * pvX + BOOST_ADD * prvX;
            pvZ = BOOST_KEEP * pvZ + BOOST_ADD * prvZ;
            pvFloor = BOOST_KEEP * pvFloor + BOOST_ADD * prvY;
            pvCeil = BOOST_KEEP * pvCeil + BOOST_ADD * prvY;
        }
        double pvVy = Math.min(pvFloor, pvCeil);
        double pvOutX = stepHorizontal(pvX, prvX, pvX, pvZ, pvVy, prvY, prvD0, input);
        double pvOutZ = stepHorizontal(pvZ, prvZ, pvX, pvZ, pvVy, prvY, prvD0, input);

        stepX = hiOutX;
        stepZ = hiOutZ;
        double countSpread = ClientMath.horizontalDistance(hiOutX - loOutX, hiOutZ - loOutZ);
        double lookSpread = ClientMath.horizontalDistance(hiOutX - pvOutX, hiOutZ - pvOutZ);
        stepRadius = countSpread + lookSpread;

        double minFloor = Math.min(loFloor, Math.min(hiFloor, pvFloor));
        double maxCeil = Math.max(loCeil, Math.max(hiCeil, pvCeil));
        stepFloor = Math.min(glideStepY(loX, loZ, minFloor, curY, curD0, input),
                Math.min(glideStepY(hiX, hiZ, minFloor, curY, curD0, input),
                        glideStepY(pvX, pvZ, minFloor, prvY, prvD0, input)));
        stepCeil = Math.max(glideStepY(loX, loZ, maxCeil, curY, curD0, input),
                Math.max(glideStepY(hiX, hiZ, maxCeil, curY, curD0, input),
                        glideStepY(pvX, pvZ, maxCeil, prvY, prvD0, input)));

        double diveVy = floor;
        for (int i = 0; i < boostMax; i++) {
            diveVy = BOOST_KEEP * diveVy - BOOST_ADD;
        }
        stepFloor = Math.min(stepFloor, (diveVy - effectiveGravity(diveVy, input)) * DRAG_V);
    }

    private double stepHorizontal(double axis, double lookAxis, double vx, double vz, double vy,
                                  double lookY, double d0, ControlEnvelope input) {
        double d1 = ClientMath.horizontalDistance(vx, vz);
        double d3 = d0 * d0;
        double v = axis;
        double vyStepped = vy + effectiveGravity(vy, input) * (-1.0 + d3 * LIFT_FACTOR);
        if (vyStepped < 0.0 && d0 > 0.0) {
            double d4 = vyStepped * -SINK_TO_FORWARD * d3;
            v += lookAxis * d4 / d0;
        }
        if (lookY > 0.0 && d0 > 0.0) {
            double d5 = d1 * lookY * PITCH_UP_COST;
            v -= lookAxis * d5 / d0;
        }
        if (d0 > 0.0) {
            v += (lookAxis / d0 * d1 - v) * STEER_RATE;
        }
        return v * DRAG_H;
    }

    private double glideStepY(double vx, double vz, double vy, double lookY, double d0, ControlEnvelope input) {
        double d1 = ClientMath.horizontalDistance(vx, vz);
        double d3 = d0 * d0;
        double v = vy + effectiveGravity(vy, input) * (-1.0 + d3 * LIFT_FACTOR);
        if (v < 0.0 && d0 > 0.0) {
            v += v * -SINK_TO_FORWARD * d3;
        }
        if (lookY > 0.0 && d0 > 0.0) {
            v += d1 * lookY * PITCH_UP_COST * PITCH_UP_LIFT;
        }
        return v * DRAG_V;
    }

    private static double effectiveGravity(double vy, ControlEnvelope input) {
        return input.slowFalling() && vy <= 0.0
                ? Math.min(input.gravity(), SLOW_FALLING_GRAVITY)
                : input.gravity();
    }
}
