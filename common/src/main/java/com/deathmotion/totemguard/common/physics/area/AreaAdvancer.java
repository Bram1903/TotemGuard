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

package com.deathmotion.totemguard.common.physics.area;

import com.deathmotion.totemguard.common.util.ClientMath;

public final class AreaAdvancer {

    private static final double ZERO_THRESHOLD = 0.003;

    private AreaAdvancer() {
    }

    public static void clampObserved(AreaBounds bounds, double obsX, double obsY, double obsZ,
                                     boolean altCenterUsed, double driftSlack) {
        if (bounds.hasSegment() && !altCenterUsed) {
            OutwardResidual.segmentCollapse(bounds, obsX, obsZ, bounds.radius() + driftSlack);
            bounds.legalVy(Math.min(Math.max(obsY, bounds.judgedFloor()), bounds.ceiling()));
            return;
        }
        double referenceX = altCenterUsed ? bounds.altCenterX() : bounds.centerX();
        double referenceZ = altCenterUsed ? bounds.altCenterZ() : bounds.centerZ();
        double reach = bounds.radius() + driftSlack;

        double adjustedX = bounds.pushAdjustedX(obsX, referenceX);
        double adjustedZ = bounds.pushAdjustedZ(obsZ, referenceZ);
        double admittedX = obsX;
        double admittedZ = obsZ;
        double deviation = OutwardResidual.deviation(obsX, obsZ, adjustedX, adjustedZ);
        if (deviation > reach && deviation > 0.0) {
            double s = reach / deviation;
            admittedX = OutwardResidual.collapseAxis(obsX, adjustedX, s);
            admittedZ = OutwardResidual.collapseAxis(obsZ, adjustedZ, s);
        }

        double velocityDeviation = OutwardResidual.deviation(admittedX, admittedZ, referenceX, referenceZ);
        if (velocityDeviation > reach && velocityDeviation > 0.0) {
            double s = reach / velocityDeviation;
            bounds.legalX(OutwardResidual.collapseAxis(admittedX, referenceX, s));
            bounds.legalZ(OutwardResidual.collapseAxis(admittedZ, referenceZ, s));
        } else {
            bounds.legalX(admittedX);
            bounds.legalZ(admittedZ);
        }
        bounds.legalVy(Math.min(Math.max(obsY, bounds.judgedFloor()), bounds.ceiling()));
    }

    public static MotionArea next(double legalX, double legalZ, double frictionH, double speedFactor,
                                  double advancedFloorVy, double advancedCeilVy) {
        return new MotionArea(legalX * speedFactor * frictionH, legalZ * speedFactor * frictionH,
                0.0, advancedFloorVy, advancedCeilVy);
    }

    public static MotionArea coast(MotionArea area, double accelBound, double frictionMax,
                                   double advancedFloor, double advancedCeil) {
        return new MotionArea(
                area.centerX() * frictionMax,
                area.centerZ() * frictionMax,
                (area.slack() + accelBound) * frictionMax,
                advancedFloor,
                advancedCeil);
    }

    public static MotionArea zeroClamp(MotionArea area, boolean jointHorizontal) {
        double centerX = area.centerX();
        double centerZ = area.centerZ();
        double slack = area.slack();
        if (jointHorizontal) {
            double horizontal = ClientMath.horizontalDistance(centerX, centerZ);
            if (horizontal > 0.0 && horizontal < ZERO_THRESHOLD) {
                slack += horizontal;
                centerX = 0.0;
                centerZ = 0.0;
            }
        } else {
            double zeroedX = Math.abs(centerX) < ZERO_THRESHOLD ? centerX : 0.0;
            double zeroedZ = Math.abs(centerZ) < ZERO_THRESHOLD ? centerZ : 0.0;
            if (zeroedX != 0.0 || zeroedZ != 0.0) {
                slack += ClientMath.horizontalDistance(zeroedX, zeroedZ);
                if (zeroedX != 0.0) centerX = 0.0;
                if (zeroedZ != 0.0) centerZ = 0.0;
            }
        }
        double floor = area.floorVy();
        double ceiling = area.ceilVy();
        if (floor > 0.0 && floor < ZERO_THRESHOLD) floor = 0.0;
        if (ceiling < 0.0 && ceiling > -ZERO_THRESHOLD) ceiling = 0.0;
        if (centerX == area.centerX() && centerZ == area.centerZ()
                && floor == area.floorVy() && ceiling == area.ceilVy()) {
            return area;
        }
        return new MotionArea(centerX, centerZ, slack, floor, ceiling);
    }
}
