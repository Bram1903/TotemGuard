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

package com.deathmotion.totemguard.api.mod;

import org.jetbrains.annotations.NotNull;

/**
 * Action applied to a mod-detection session at resolution, after collapsing every
 * detected mod's {@link ModSeverity} into one outcome.
 */
public enum ModAction {

    /**
     * No disallowed mods, or all detected mods are {@link ModSeverity#LOG}. Staff alert
     * fires but the player is not disconnected.
     */
    NONE(""),

    /**
     * Player was kicked. Kick message lists detected mods (capped by display limit).
     */
    KICK("K"),

    /**
     * Player was kicked and a kick-then-ban warning was recorded. A re-join in the
     * warning window with any warned mod escalates to {@link #BAN}, a brand-new
     * disallowed mod is treated as a fresh first offense.
     */
    KICK_THEN_BAN("KB"),

    /**
     * Player was banned. Ban message lists detected mods (capped by display limit).
     */
    BAN("B");

    private final String shortLabel;

    ModAction(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    /**
     * Compact inline-alert label: {@code K}, {@code KB}, {@code B}, or empty for {@link #NONE}.
     */
    public @NotNull String shortLabel() {
        return shortLabel;
    }
}
