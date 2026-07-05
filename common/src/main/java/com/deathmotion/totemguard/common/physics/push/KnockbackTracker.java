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

package com.deathmotion.totemguard.common.physics.push;

import com.deathmotion.totemguard.common.physics.area.JudgedExcess;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;

public final class KnockbackTracker {

    private final ExternalVelocityData external;

    public KnockbackTracker(ExternalVelocityData external) {
        this.external = external;
    }

    public void apply(AreaBounds bounds, double knockbackPad) {
        if (!external.isActive()) return;
        bounds.altCenter(bounds.centerX() + external.x(), bounds.centerZ() + external.z());
        bounds.expandRadius(knockbackPad);
        double up = Math.max(0.0, external.y());
        if (up > 0.0) bounds.ceiling(bounds.ceiling() + up + knockbackPad);
        double down = Math.max(0.0, -external.y());
        if (down > 0.0) bounds.addDescentSlack(down + knockbackPad);
    }

    public void consumeIfExplained(JudgedExcess excess, double horizontalEpsilon, double verticalEpsilon) {
        if (!external.isActive()) return;
        if (excess.altCenterUsed() && excess.horizontal() <= horizontalEpsilon
                && excess.ascent() <= verticalEpsilon) {
            external.consume();
        }
    }

    public boolean active() {
        return external.isActive();
    }
}
