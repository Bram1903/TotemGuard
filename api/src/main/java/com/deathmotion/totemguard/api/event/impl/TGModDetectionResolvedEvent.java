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

package com.deathmotion.totemguard.api.event.impl;

import com.deathmotion.totemguard.api.event.Cancellable;
import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Fired when a player's mod-detection session resolves into a single action,
 * carrying the full set of disallowed mods that triggered the resolution.
 * <p>
 * One event per resolution, not one per detected mod. If a disallowed mod is
 * detected after the session has already resolved (for example, the player
 * loads a mod mid-session) a follow-up event is fired with {@link #isLate()}
 * returning {@code true}; the mod set on a late event contains only the newly
 * observed mod.
 * <p>
 * Cancelling this event suppresses the staff alert, the punishment dispatch,
 * and the kick-then-ban warning bookkeeping for this resolution. The
 * subsequent {@link TGUserFlagEvent} fired through the standard check
 * pipeline is also skipped because the alert path is never invoked.
 */
public interface TGModDetectionResolvedEvent extends TGUserEvent, Cancellable {

    /**
     * The disallowed mods that triggered this resolution.
     * <p>
     * For non-late events this is the full set accumulated during the session
     * up to the boundary tick. For late events it contains only the newly
     * observed mod or mods.
     *
     * @return the detected mods, never {@code null}
     */
    @NotNull Set<DetectedMod> getDetectedMods();

    /**
     * The action TotemGuard will apply when the event completes uncancelled.
     *
     * @return the resolved action, never {@code null}
     */
    @NotNull ModAction getAction();

    /**
     * Whether this event was fired for a detection that arrived after the
     * session had already resolved.
     *
     * @return {@code true} if this is a post-resolve delta event
     */
    boolean isLate();
}
