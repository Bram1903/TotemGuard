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

package com.deathmotion.totemguard.common.physics.preset;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum PhysicsDebugContext {
    LAND,
    WATER,
    LAVA,
    CLIMB,
    GLIDE,
    VEHICLE,
    FLUIDBOX;

    public static Set<PhysicsDebugContext> parse(List<String> raw) {
        EnumSet<PhysicsDebugContext> set = EnumSet.noneOf(PhysicsDebugContext.class);
        if (raw == null) return set;
        for (String entry : raw) {
            PhysicsDebugContext context = parseOne(entry);
            if (context != null) set.add(context);
        }
        return set;
    }

    private static PhysicsDebugContext parseOne(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase()) {
            case "land", "walk", "ground", "air" -> LAND;
            case "water", "swim", "swimming" -> WATER;
            case "lava" -> LAVA;
            case "climb", "climbing", "ladder", "vine" -> CLIMB;
            case "glide", "gliding", "elytra" -> GLIDE;
            case "vehicle", "vehicles", "boat" -> VEHICLE;
            case "fluidbox", "fluid-box", "membership" -> FLUIDBOX;
            default -> null;
        };
    }
}
