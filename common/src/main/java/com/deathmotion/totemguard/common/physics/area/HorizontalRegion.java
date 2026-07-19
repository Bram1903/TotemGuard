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

public final class HorizontalRegion {

    private HorizontalRegion() {
    }

    public static double primaryExcess(AreaBounds bounds, double obsX, double obsZ) {
        if (bounds.hasSegment()) {
            return OutwardResidual.excess(obsX, obsZ,
                    bounds.pushAdjustedX(obsX, segmentBestX(bounds, obsX)),
                    bounds.pushAdjustedZ(obsZ, segmentBestZ(bounds, obsZ)),
                    bounds.radius());
        }
        return OutwardResidual.excess(obsX, obsZ,
                bounds.pushAdjustedX(obsX, bounds.centerX()),
                bounds.pushAdjustedZ(obsZ, bounds.centerZ()), bounds.radius());
    }

    public static double altExcess(AreaBounds bounds, double obsX, double obsZ) {
        return OutwardResidual.excess(obsX, obsZ,
                bounds.pushAdjustedX(obsX, bounds.altCenterX()),
                bounds.pushAdjustedZ(obsZ, bounds.altCenterZ()), bounds.radius());
    }

    public static void clampToLegal(AreaBounds bounds, double obsX, double obsZ,
                                    boolean altCenterUsed, double reach) {
        double referenceX;
        double referenceZ;
        if (bounds.hasSegment() && !altCenterUsed) {
            referenceX = segmentBestX(bounds, obsX);
            referenceZ = segmentBestZ(bounds, obsZ);
        } else if (altCenterUsed) {
            referenceX = bounds.altCenterX();
            referenceZ = bounds.altCenterZ();
        } else {
            referenceX = bounds.centerX();
            referenceZ = bounds.centerZ();
        }

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
    }

    private static double segmentBestX(AreaBounds bounds, double obs) {
        double lo = bounds.centerX() + bounds.segDirX() * bounds.segMin();
        double hi = bounds.centerX() + bounds.segDirX() * bounds.segMax();
        return OutwardResidual.outward(obs, bounds.pushAdjustedX(obs, lo))
                <= OutwardResidual.outward(obs, bounds.pushAdjustedX(obs, hi)) ? lo : hi;
    }

    private static double segmentBestZ(AreaBounds bounds, double obs) {
        double lo = bounds.centerZ() + bounds.segDirZ() * bounds.segMin();
        double hi = bounds.centerZ() + bounds.segDirZ() * bounds.segMax();
        return OutwardResidual.outward(obs, bounds.pushAdjustedZ(obs, lo))
                <= OutwardResidual.outward(obs, bounds.pushAdjustedZ(obs, hi)) ? lo : hi;
    }
}
