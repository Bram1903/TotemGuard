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

package com.deathmotion.totemguard.common.world.block;

import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Accessors(fluent = true)
@Getter
public final class DimensionProfile {

    private String worldName;
    private DimensionType dimensionType;
    private int minY;

    public void update(String worldName, DimensionType dimensionType, int minY) {
        this.worldName = worldName;
        this.dimensionType = dimensionType;
        this.minY = minY;
    }

    public void reset() {
        worldName = null;
        dimensionType = null;
        minY = 0;
    }

    public boolean isWorldChange(WrapperPlayServerRespawn respawn) {
        if (respawn.getWorldName().isPresent()) {
            return !Objects.equals(respawn.getWorldName().orElse(null), worldName);
        }
        return !Objects.equals(respawn.getDimensionType(), dimensionType);
    }
}
