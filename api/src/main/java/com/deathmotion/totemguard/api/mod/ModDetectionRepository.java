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
 * Read-only access to the mod detection subsystem. Detection is decoupled from the
 * check pipeline. Each online player has at most one session that resolves on a tick
 * boundary into a {@link com.deathmotion.totemguard.api.event.events.TGModDetectionResolvedEvent}.
 * Detections are not persisted, accessors are synchronous in-memory lookups against
 * online players, offline UUIDs return empty or {@code null}.
 */
public interface ModDetectionRepository {

    /**
     * Immutable snapshot of disallowed mods detected this session. May grow until the
     * session resolves and during the post-resolve late pathway. Empty if no active session.
     */
    @NotNull Set<DetectedMod> getDetectedMods(@NotNull UUID uuid);

    /**
     * Whether the player has been observed with {@code modId} this session.
     */
    boolean hasDetectedMod(@NotNull UUID uuid, @NotNull String modId);

    /**
     * Whether the session has resolved. Detections arriving after this take the late
     * pathway and dispatch as delta events.
     */
    boolean isSessionResolved(@NotNull UUID uuid);

    /**
     * The resolved action, or {@code null} if no session has resolved for this UUID.
     */
    @Nullable ModAction getResolvedAction(@NotNull UUID uuid);
}
