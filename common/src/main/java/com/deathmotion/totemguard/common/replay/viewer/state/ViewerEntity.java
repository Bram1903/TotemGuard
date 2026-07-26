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

package com.deathmotion.totemguard.common.replay.viewer.state;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class ViewerEntity {

    private static final int MAX_EFFECTS = 32;

    private final RetainedPacket spawn;
    private final Map<String, RetainedPacket> effects = new LinkedHashMap<>();

    private double x;
    private double y;
    private double z;
    private boolean positioned;

    @Setter
    private @Nullable RetainedPacket metadata;
    @Setter
    private @Nullable RetainedPacket equipment;
    @Setter
    private @Nullable RetainedPacket passengers;
    @Setter
    private @Nullable RetainedPacket attributes;

    ViewerEntity(RetainedPacket spawn) {
        this.spawn = spawn;
    }

    public void positionedAt(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.positioned = true;
    }

    public void effect(String effect, RetainedPacket packet) {
        if (effects.size() >= MAX_EFFECTS && !effects.containsKey(effect)) return;
        effects.put(effect, packet);
    }

    public void removeEffect(String effect) {
        effects.remove(effect);
    }
}
