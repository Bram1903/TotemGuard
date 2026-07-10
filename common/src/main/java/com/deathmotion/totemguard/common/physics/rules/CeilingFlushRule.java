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
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;

public final class CeilingFlushRule {

    private static final double CEILING_FLUSH_EPS = 1.0e-6;
    private static final double SUPPORT_CONTACT_EPS = 0.02;

    private CeilingFlushRule() {
    }

    public static double advanceFloor(double anchor, double advancedVy, MediumModel medium,
                                      ControlEnvelope input, AreaBounds bounds, ContactReport contact) {
        if (anchor > 0.0 && contact.ceilingClearance() <= CEILING_FLUSH_EPS) {
            return medium.advanceVertical(0.0, input);
        }
        if (bounds.legalVy() < anchor && contact.nearestSupportGap() > SUPPORT_CONTACT_EPS) {
            return Math.min(advancedVy, medium.advanceVertical(bounds.legalVy(), input));
        }
        return advancedVy;
    }
}
