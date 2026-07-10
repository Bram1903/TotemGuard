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

public final class AreaJudge {

    private AreaJudge() {
    }

    public static JudgedExcess judge(AreaBounds bounds, double obsX, double obsY, double obsZ, double arrestGapCap) {
        boolean altUsed = false;
        double horizontal;
        if (bounds.hasSegment()) {
            horizontal = OutwardResidual.segmentExcess(obsX, obsZ, bounds.centerX(), bounds.centerZ(),
                    bounds.segDirX(), bounds.segDirZ(), bounds.segMin(), bounds.segMax(), bounds.radius());
        } else {
            double mainExcess = OutwardResidual.excess(obsX, obsZ, bounds.centerX(), bounds.centerZ(), bounds.radius());
            horizontal = mainExcess;
            if (bounds.hasAltCenter() && mainExcess > 0.0) {
                double altExcess = OutwardResidual.excess(obsX, obsZ, bounds.altCenterX(), bounds.altCenterZ(), bounds.radius());
                if (altExcess < mainExcess) {
                    horizontal = altExcess;
                    altUsed = true;
                }
            }
        }
        horizontal = Math.max(0.0, horizontal);

        double ascent = Math.max(0.0, obsY - bounds.ceiling());
        if (ascent > 0.0 && obsY <= 0.0 && arrestGapCap >= 0.0) {
            ascent = Math.min(ascent, arrestGapCap);
        }

        double descent = 0.0;
        if (bounds.enforceDescentFloor() && bounds.floor() <= 0.0) {
            descent = Math.max(0.0, (bounds.floor() - bounds.descentSlack()) - obsY);
        }

        return new JudgedExcess(horizontal, ascent, descent, altUsed);
    }
}
