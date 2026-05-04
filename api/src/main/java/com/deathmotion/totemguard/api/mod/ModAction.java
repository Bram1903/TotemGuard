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
 * The action TotemGuard actually applied to a mod-detection session at resolution
 * time, after collapsing every detected mod's {@link ModSeverity} down to a single
 * outcome.
 */
public enum ModAction {

    /**
     * No disallowed mods were detected, or every detected mod has severity
     * {@link ModSeverity#LOG}. Staff still receive an alert; the player is not
     * disconnected.
     */
    NONE(""),

    /**
     * The player was kicked. The kick message lists every disallowed mod from the
     * resolved set (capped by the configured display limit).
     */
    KICK("K"),

    /**
     * The player was kicked and a kick-then-ban warning was recorded. A re-join inside
     * the configured warning window with any of the warned mods escalates to
     * {@link #BAN}; a re-join with a brand-new disallowed mod is treated as a fresh
     * first offense.
     */
    KICK_THEN_BAN("KB"),

    /**
     * The player was banned. The ban message lists every disallowed mod from the
     * resolved set (capped by the configured display limit).
     */
    BAN("B");

    private final String shortLabel;

    ModAction(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    /**
     * Compact display label suitable for inline staff alerts: {@code "K"} for
     * {@link #KICK}, {@code "KB"} for {@link #KICK_THEN_BAN}, {@code "B"} for
     * {@link #BAN}, and the empty string for {@link #NONE}.
     *
     * @return the short label, never {@code null}
     */
    public @NotNull String shortLabel() {
        return shortLabel;
    }
}
