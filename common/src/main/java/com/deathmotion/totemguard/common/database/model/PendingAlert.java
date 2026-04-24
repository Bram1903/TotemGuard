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

package com.deathmotion.totemguard.common.database.model;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable row sitting in the alert writer queue.
 *
 * <p>{@code sessionId} is nullable for rare cases where a flag fires before
 * the session row has finished inserting — in that path we still record the
 * alert with the player context so history queries remain accurate, just
 * without the session linkage.</p>
 *
 * <p>The two ping values are captured from the player at flag-time so the
 * history view can show latency context per-alert rather than "whatever the
 * player's ping is right now".</p>
 */
public record PendingAlert(
        @Nullable Long sessionId,
        int playerId,
        int serverId,
        int checkId,
        int violations,
        @Nullable String debug,
        @Nullable Integer keepalivePing,
        @Nullable Integer transactionPing,
        long createdAt
) {
}
