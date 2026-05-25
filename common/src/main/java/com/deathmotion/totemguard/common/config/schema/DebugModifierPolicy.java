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

package com.deathmotion.totemguard.common.config.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// How the inventory checks treat movement that is reachable through the vanilla
// debug-modifier client bug (walking while an inventory is open). Unknown or blank
// values fall back to PUNISH, so a malformed config never silently opens the bypass.
public enum DebugModifierPolicy {
    // Flag, mitigate, and punish it (default).
    PUNISH,
    // Flag and mitigate but never punish.
    FLAG,
    // Do not flag it at all.
    IGNORE;

    public static @NotNull DebugModifierPolicy parse(@Nullable String raw) {
        if (raw == null) return PUNISH;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return PUNISH;
        }
    }
}
