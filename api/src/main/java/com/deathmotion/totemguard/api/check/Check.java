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

package com.deathmotion.totemguard.api.check;

import com.deathmotion.totemguard.api.reload.Reloadable;
import org.jetbrains.annotations.NotNull;

/**
 * A single check. Identifies suspicious behavior and may trigger flag events when its
 * conditions are met.
 */
public interface Check extends Reloadable {

    /**
     * Stable identifier from {@code @CheckData(name = ...)}. Used as the alert prefix, the
     * bypass permission suffix ({@code TotemGuard.Bypass.<name>}), and the history filter
     * key, so it must remain stable across releases.
     */
    @NotNull String getName();

    /**
     * One-line summary from {@code @CheckData(description = ...)}, rendered in admin GUIs.
     */
    @NotNull String getDescription();

    /**
     * Category bucket from {@code @CheckData(type = ...)}, drives grouping in alerts and history.
     */
    @NotNull CheckType getType();

    /**
     * Whether this check is experimental. Experimental checks may be incomplete or
     * unstable and should not be enabled by default in production.
     */
    boolean isExperimental();

    /**
     * Live snapshot of the per-instance enable flag from {@code checks.yml}, reflects reloads.
     */
    boolean isEnabled();

    /**
     * Whether this check is marked {@code @RequiresTickEnd}, meaning it must run inside the
     * tick-end pass rather than at packet receive time.
     */
    boolean requiresTickEnd();

    /**
     * Whether this check is heuristic-based (buffer plus decay plus sanity guards) rather
     * than a deterministic packet or state assertion.
     */
    boolean isHeuristic();

    /**
     * Running violation count since the player was bound to this check instance, reset on
     * player quit. Not the same as historical violations in the database.
     */
    int getViolations();
}
