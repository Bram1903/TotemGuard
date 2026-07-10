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

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.GlideData;

public final class RiptideWindow {

    private static final double GROUND_HOP = 1.1999999;
    private static final double SPIN_RECOIL = -0.2;

    private final Data data;
    private final GlideData glide;

    public RiptideWindow(Data data) {
        this.data = data;
        this.glide = data.getGlideData();
    }

    public void apply(AreaBounds bounds, ControlEnvelope input) {
        if (glide.riptideActive()) {
            double strength = glide.riptideStrength();
            if (!bounds.hasAltCenter()) {
                bounds.altCenter(bounds.centerX() + input.lookX() * strength,
                        bounds.centerZ() + input.lookZ() * strength);
            } else {
                bounds.expandRadius(strength);
            }
            if (!data.isGliding()) {
                double vertical = input.lookY() * strength;
                if (vertical > 0.0) {
                    bounds.ceiling(bounds.ceiling() + vertical);
                } else if (vertical < 0.0) {
                    bounds.lowerFloor(bounds.floor() + vertical);
                }
            }
            if (glide.riptideGrounded()) {
                bounds.ceiling(bounds.ceiling() + GROUND_HOP);
            }
            return;
        }
        if (data.isSpinAttacking() && !bounds.hasAltCenter()) {
            bounds.altCenter(bounds.centerX() * SPIN_RECOIL, bounds.centerZ() * SPIN_RECOIL);
        }
    }
}
