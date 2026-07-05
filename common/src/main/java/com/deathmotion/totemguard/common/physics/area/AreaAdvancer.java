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

public final class AreaAdvancer {

    private AreaAdvancer() {
    }

    public static void clampObserved(AreaBounds bounds, double obsX, double obsY, double obsZ,
                                     boolean altCenterUsed, double driftSlack) {
        double centerX = altCenterUsed ? bounds.altCenterX() : bounds.centerX();
        double centerZ = altCenterUsed ? bounds.altCenterZ() : bounds.centerZ();
        double reach = bounds.radius() + driftSlack;
        double deviation = OutwardResidual.deviation(obsX, obsZ, centerX, centerZ);
        if (deviation <= reach || deviation <= 0.0) {
            bounds.legalX(obsX);
            bounds.legalZ(obsZ);
        } else {
            double s = reach / deviation;
            bounds.legalX(OutwardResidual.collapseAxis(obsX, centerX, s));
            bounds.legalZ(OutwardResidual.collapseAxis(obsZ, centerZ, s));
        }
        double floor = bounds.floor() - bounds.descentSlack();
        bounds.legalVy(Math.min(Math.max(obsY, floor), bounds.ceiling()));
    }

    public static MotionArea next(double legalX, double legalZ, double frictionH, double speedFactor,
                                      double advancedVy) {
        double f = frictionH * speedFactor;
        return new MotionArea(legalX * f, legalZ * f, 0.0, advancedVy, advancedVy);
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
}
