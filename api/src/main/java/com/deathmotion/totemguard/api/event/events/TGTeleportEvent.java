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
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired on {@code /tg teleport <player>}. Cancelling aborts before any local hop or
 * cross-server route is published. The caller is identified by UUID only since admins
 * running this command typically bypass registration as a {@link TGUser}.
 */
public interface TGTeleportEvent extends TGEvent, Cancellable {

    /**
     * UUID of the staff member running the teleport command.
     */
    @NotNull UUID getCallerUuid();

    /**
     * UUID of the player being teleported to.
     */
    @NotNull UUID getTargetUuid();

    /**
     * Display name of the player being teleported to.
     */
    @NotNull String getTargetName();

    /**
     * The target's {@link TGUser} when online on this server, {@code null} for cross-server targets.
     */
    @Nullable TGUser getTargetUser();

    /**
     * Stable per-instance UUID of the backend hosting the target. Equals this server's
     * instance id when {@link #isCrossServer()} is {@code false}.
     */
    @NotNull UUID getTargetServerInstanceId();

    /**
     * TotemGuard's friendly identity for the server hosting the target.
     */
    @NotNull String getTargetServerName();

    /**
     * Whether the target is on a different backend and the teleport needs a server hop.
     */
    boolean isCrossServer();
}
