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

package com.deathmotion.totemguard.common.replay.viewer.net;

import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record RecordedWorld(DimensionTypeRef dimensionRef, DimensionType dimensionType, long hashedSeed,
                            boolean debug, boolean flat, int seaLevel, int viewDistance) {

    public int sectionCount() {
        return Math.max(1, dimensionType.getHeight() >> 4);
    }

    public boolean sameWorldAs(@Nullable RecordedWorld other) {
        if (other == null) return false;
        if (debug != other.debug || flat != other.flat || seaLevel != other.seaLevel) return false;

        ResourceLocation name = dimensionType.getName();
        ResourceLocation theirs = other.dimensionType.getName();
        if (name != null && theirs != null) return Objects.equals(name, theirs);

        return dimensionType.getHeight() == other.dimensionType.getHeight()
                && dimensionType.getMinY() == other.dimensionType.getMinY();
    }
}
