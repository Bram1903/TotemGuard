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

package com.deathmotion.totemguard.api.event.events;

import com.deathmotion.totemguard.api.event.Cancellable;
import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Fired when a player's mod-detection session resolves. One event per resolution, not
 * per mod. Mods observed after the session resolved fire a follow-up event with
 * {@link #isLate()} {@code true} containing only the new mods. Cancelling suppresses
 * the staff alert, punishment dispatch, kick-then-ban bookkeeping, and the downstream
 * {@link TGUserFlagEvent}.
 */
public interface TGModDetectionResolvedEvent extends TGUserEvent, Cancellable {

    /**
     * The disallowed mods that triggered this resolution.
     */
    @NotNull Set<DetectedMod> getDetectedMods();

    /**
     * The action TotemGuard will apply if the event is not cancelled.
     */
    @NotNull ModAction getAction();

    /**
     * Whether this fired for mods observed after the session boundary tick resolved.
     */
    boolean isLate();
}
