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

package com.deathmotion.totemguard.common.physics.rules;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.area.JudgedExcess;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;

public final class BedBounceRule {

    private static final int BOUNCE_WINDOW = 4;

    private double altCenterX;
    private double altCenterZ;
    private int window;
    private boolean valid;
    private boolean usedLast;
    private boolean prepared;

    public void prepare(boolean landMedium) {
        prepared = landMedium && valid && !usedLast;
    }

    public boolean applyTo(AreaBounds bounds, double inputShiftX, double inputShiftZ) {
        if (!prepared || bounds.hasAltCenter()) return false;
        bounds.altCenter(altCenterX + inputShiftX, altCenterZ + inputShiftZ);
        return true;
    }

    public void onJudged(boolean offered, JudgedExcess excess) {
        usedLast = offered && excess.altCenterUsed();
    }

    public void arm(ContactReport contact, AreaBounds bounds) {
        if (contact.supportBounce() > 0.0) {
            window = BOUNCE_WINDOW;
        } else if (window > 0) {
            window--;
        }
        if (window > 0) {
            altCenterX = bounds.legalX() * LandModel.AIR_FRICTION;
            altCenterZ = bounds.legalZ() * LandModel.AIR_FRICTION;
            valid = true;
        } else {
            valid = false;
        }
    }

    public void reset() {
        valid = false;
        usedLast = false;
        window = 0;
        prepared = false;
    }
}
