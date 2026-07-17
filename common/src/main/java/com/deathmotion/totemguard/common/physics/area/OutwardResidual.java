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
        return excess(obsX, obsZ,
                bounds.pushAdjustedX(obsX, segmentBestX(bounds, obsX)),
                bounds.pushAdjustedZ(obsZ, segmentBestZ(bounds, obsZ)),
                bounds.radius());
    }

    public static void segmentCollapse(AreaBounds bounds, double obsX, double obsZ, double reach) {
        double rawX = segmentBestX(bounds, obsX);
        double rawZ = segmentBestZ(bounds, obsZ);
        double adjustedX = bounds.pushAdjustedX(obsX, rawX);
        double adjustedZ = bounds.pushAdjustedZ(obsZ, rawZ);
        double admittedX = obsX;
        double admittedZ = obsZ;
        double deviation = deviation(obsX, obsZ, adjustedX, adjustedZ);
        if (deviation > reach && deviation > 0.0) {
            double s = reach / deviation;
            admittedX = collapseAxis(obsX, adjustedX, s);
            admittedZ = collapseAxis(obsZ, adjustedZ, s);
        }
        double velocityDeviation = deviation(admittedX, admittedZ, rawX, rawZ);
        if (velocityDeviation > reach && velocityDeviation > 0.0) {
            double s = reach / velocityDeviation;
            bounds.legalX(collapseAxis(admittedX, rawX, s));
            bounds.legalZ(collapseAxis(admittedZ, rawZ, s));
        } else {
            bounds.legalX(admittedX);
            bounds.legalZ(admittedZ);
        }
    }

    private static double segmentBestX(AreaBounds bounds, double obs) {
        double lo = bounds.centerX() + bounds.segDirX() * bounds.segMin();
        double hi = bounds.centerX() + bounds.segDirX() * bounds.segMax();
        return outward(obs, bounds.pushAdjustedX(obs, lo))
                <= outward(obs, bounds.pushAdjustedX(obs, hi)) ? lo : hi;
    }

    private static double segmentBestZ(AreaBounds bounds, double obs) {
        double lo = bounds.centerZ() + bounds.segDirZ() * bounds.segMin();
        double hi = bounds.centerZ() + bounds.segDirZ() * bounds.segMax();
        return outward(obs, bounds.pushAdjustedZ(obs, lo))
                <= outward(obs, bounds.pushAdjustedZ(obs, hi)) ? lo : hi;
    }
}
