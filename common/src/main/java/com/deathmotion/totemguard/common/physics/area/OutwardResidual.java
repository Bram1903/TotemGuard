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

public final class OutwardResidual {

    private OutwardResidual() {
    }

    public static double outward(double observed, double center) {
        if (center > 0.0) {
            return observed < 0.0 ? -observed : Math.max(0.0, observed - center);
        }
        if (center < 0.0) {
            return observed > 0.0 ? observed : Math.max(0.0, center - observed);
        }
        return Math.abs(observed);
    }

    public static double deviation(double obsX, double obsZ, double centerX, double centerZ) {
        return ClientMath.horizontalDistance(outward(obsX, centerX), outward(obsZ, centerZ));
    }

    public static double excess(double obsX, double obsZ, double centerX, double centerZ, double radius) {
        return deviation(obsX, obsZ, centerX, centerZ) - radius;
    }

    public static double collapseAxis(double observed, double center, double s) {
        if (center > 0.0) {
            if (observed < 0.0) return observed * s;
            if (observed > center) return center + (observed - center) * s;
            return observed;
        }
        if (center < 0.0) {
            if (observed > 0.0) return observed * s;
            if (observed < center) return center + (observed - center) * s;
            return observed;
        }
        return observed * s;
    }

    public static double segmentExcess(AreaBounds bounds, double obsX, double obsZ) {
        double along = segmentAlong(obsX, obsZ, bounds.centerX(), bounds.centerZ(),
                bounds.segDirX(), bounds.segDirZ(), bounds.segMin(), bounds.segMax());
        double closestX = bounds.pushAdjustedX(obsX, bounds.centerX() + bounds.segDirX() * along);
        double closestZ = bounds.pushAdjustedZ(obsZ, bounds.centerZ() + bounds.segDirZ() * along);
        return ClientMath.horizontalDistance(obsX - closestX, obsZ - closestZ) - bounds.radius();
    }

    public static void segmentClosest(AreaBounds bounds, double obsX, double obsZ) {
        double along = segmentAlong(obsX, obsZ, bounds.centerX(), bounds.centerZ(),
                bounds.segDirX(), bounds.segDirZ(), bounds.segMin(), bounds.segMax());
        bounds.segClosestX(bounds.pushAdjustedX(obsX, bounds.centerX() + bounds.segDirX() * along));
        bounds.segClosestZ(bounds.pushAdjustedZ(obsZ, bounds.centerZ() + bounds.segDirZ() * along));
    }

    public static void segmentCollapse(AreaBounds bounds, double obsX, double obsZ, double reach) {
        double closestX = bounds.segClosestX();
        double closestZ = bounds.segClosestZ();
        double deviation = deviation(obsX, obsZ, closestX, closestZ);
        if (deviation <= reach || deviation <= 0.0) {
            bounds.legalX(obsX);
            bounds.legalZ(obsZ);
            return;
        }
        double s = reach / deviation;
        bounds.legalX(collapseAxis(obsX, closestX, s));
        bounds.legalZ(collapseAxis(obsZ, closestZ, s));
    }

    private static double segmentAlong(double obsX, double obsZ, double centerX, double centerZ,
                                       double dirX, double dirZ, double reachMin, double reachMax) {
        double along = (obsX - centerX) * dirX + (obsZ - centerZ) * dirZ;
        return Math.max(reachMin, Math.min(reachMax, along));
    }
}
