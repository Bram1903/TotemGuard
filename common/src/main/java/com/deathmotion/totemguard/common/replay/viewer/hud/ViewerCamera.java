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

package com.deathmotion.totemguard.common.replay.viewer.hud;

import lombok.Getter;

@Getter
public enum ViewerCamera {

    FREE("free", "Fly wherever you like"),
    FOLLOW("follow", "Trail the recorded player from behind"),
    EYES("eyes", "See it from the recorded player's head");

    private final String label;
    private final String description;

    ViewerCamera(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public ViewerCamera next() {
        ViewerCamera[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
