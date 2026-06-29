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

package com.deathmotion.totemguard.common.player.data;

import com.deathmotion.totemguard.common.player.movement.MovementConstants;

public class PlayerAttributeData {

    private double scale = 1.0;

    public void setScale(double scale) {
        this.scale = Math.max(0.0625, Math.min(16.0, scale));
    }

    public double width() {
        return MovementConstants.BASE_WIDTH * scale;
    }

    public double height() {
        return MovementConstants.STANDING_HEIGHT * scale;
    }

    public double scale() {
        return scale;
    }

    public void reset() {
        scale = 1.0;
    }
}
