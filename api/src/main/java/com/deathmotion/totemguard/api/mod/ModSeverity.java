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

/**
 * Severity declared by an entry in {@code mods.yml}.
 * <p>
 * The severity is a per-mod policy, but resolution is per-session: when a session
 * accumulates multiple disallowed mods, TotemGuard collapses them down to a single
 * outcome by picking the highest severity in the set, with this ordering (highest
 * first): {@link #BAN}, {@link #KICK_THEN_BAN}, {@link #KICK}, {@link #LOG}.
 * <p>
 * For the action that was actually applied to a session see {@link ModAction}; the
 * two are not the same in {@link #KICK_THEN_BAN}, where the recorded action depends
 * on whether the player was previously warned within the configured window.
 */
public enum ModSeverity {

    /**
     * Detection is logged and dispatched to staff but no punishment is issued.
     */
    LOG,

    /**
     * The player is kicked. Re-joining is permitted.
     */
    KICK,

    /**
     * The player is banned outright on the first detection.
     */
    BAN,

    /**
     * The player is kicked the first time the mod is detected, then banned if any
     * disallowed mod from the same warning set is still present on a re-join inside
     * the configured warning window.
     * <p>
     * A brand-new disallowed mod (one not in the warning set) is treated as a fresh
     * first offense and triggers another kick rather than a ban.
     */
    KICK_THEN_BAN
}
