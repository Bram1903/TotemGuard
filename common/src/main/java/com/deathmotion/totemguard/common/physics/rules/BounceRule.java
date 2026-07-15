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

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.world.block.BlockTraits;

public final class BounceRule {

    private BounceRule() {
    }

    public static double reflectMax(boolean restitutionBounce, ContactReport contact,
                                    double fallVy, double gravity) {
        return reflectMax(restitutionBounce, contact, fallVy, gravity, MotionDefaults.VERTICAL_DRAG);
    }

    public static double reflectMax(boolean restitutionBounce, ContactReport contact,
                                    double fallVy, double gravity, double airDrag) {
        double factor = contact.supportBounce();
        if (factor <= 0.0 || fallVy >= 0.0) return 0.0;
        if (restitutionBounce) {
            if (contact.supportBounceBed()) factor = BlockTraits.BED_RESTITUTION;
            return (-fallVy + gravity) * airDrag * factor;
        }
        return -fallVy * factor;
    }

    public static double reflectMin(boolean restitutionBounce, double factor, boolean bed,
                                    double fallVy, double gravity) {
        if (factor <= 0.0 || fallVy >= 0.0) return 0.0;
        if (restitutionBounce) {
            if (-fallVy < gravity) return 0.0;
            if (bed) factor = BlockTraits.BED_RESTITUTION;
        }
        return -fallVy * factor;
    }
}
