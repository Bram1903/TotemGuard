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
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Read-only access to the mod detection subsystem.
 * <p>
 * Mod detection is intentionally decoupled from the per-tick check pipeline. Each
 * online player has at most one active session that accumulates detections from
 * plugin-channel registrations, plugin-messages, and translation probes, then
 * resolves on the next tick boundary into a single
 * {@link com.deathmotion.totemguard.api.event.impl.TGModDetectionResolvedEvent}.
 * <p>
 * Detections are not persisted: every accessor on this repository is a synchronous,
 * in-memory lookup against the currently online player set. UUIDs that are offline
 * or have not yet finished joining return empty / {@code null} results.
 */
public interface ModDetectionRepository {

    /**
     * Returns the immutable snapshot of disallowed mods detected for the given
     * player so far during their current session.
     * <p>
     * The set may grow until the session resolves and may continue to grow in the
     * post-resolve {@code late} pathway. It does not persist across re-joins.
     *
     * @param uuid the UUID of the player
     * @return the detected mods, or an empty set if the player has no active session
     */
    @NotNull Set<DetectedMod> getDetectedMods(@NotNull UUID uuid);

    /**
     * Returns whether the given player has been observed with the given mod during
     * their current session.
     *
     * @param uuid  the UUID of the player
     * @param modId the mod identifier as declared in {@code mods.yml}
     * @return {@code true} if the mod was detected for that player this session
     */
    boolean hasDetectedMod(@NotNull UUID uuid, @NotNull String modId);

    /**
     * Returns whether the given player's detection session has resolved (the
     * boundary tick after the probe burst has fired and the resolver has run).
     * Detections that arrive after this point follow the {@code late} pathway and
     * dispatch as delta events.
     *
     * @param uuid the UUID of the player
     * @return {@code true} if the session exists and has resolved
     */
    boolean isSessionResolved(@NotNull UUID uuid);

    /**
     * Returns the action that was applied when the given player's session
     * resolved, or {@code null} if no session has resolved yet for that UUID.
     *
     * @param uuid the UUID of the player
     * @return the resolved action, or {@code null} if not yet resolved
     */
    @Nullable ModAction getResolvedAction(@NotNull UUID uuid);
}
