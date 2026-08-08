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
 * Per-mod severity from {@code mods.yml}. Session resolution collapses multiple detected
 * mods to one outcome by picking the highest in this order: {@link #BAN},
 * {@link #KICK_THEN_BAN}, {@link #KICK}, {@link #LOG}. The applied {@link ModAction} can
 * differ from severity for {@link #KICK_THEN_BAN} based on the warning window.
 */
public enum ModSeverity {

    /**
     * Logged and alerted to staff, no punishment.
     */
    LOG,

    /**
     * Player is kicked. Re-joining is permitted.
     */
    KICK,

    /**
     * Player is banned outright on the first detection.
     */
    BAN,

    /**
     * Player is kicked the first time, then banned on re-join inside the warning window
     * if any disallowed mod from the warning set is still present. A brand-new disallowed
     * mod is treated as a fresh first offense and kicks again rather than bans.
     */
    KICK_THEN_BAN
}
