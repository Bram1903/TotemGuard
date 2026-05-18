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
 * Fired when a staff member runs {@code /tg teleport <player>}. Cancelling
 * aborts the teleport before any local hop or cross-server route is published.
 * <p>
 * The caller is identified by UUID only. Admins running this command
 * typically hold {@code TotemGuard.Bypass} and are not registered as a
 * {@link TGUser}.
 */
public interface TGTeleportEvent extends TGEvent, Cancellable {

    @NotNull UUID getCallerUuid();

    @NotNull UUID getTargetUuid();

    @NotNull String getTargetName();

    /**
     * The target's {@link TGUser} handle when online on this server.
     * {@code null} for cross-server targets.
     */
    @Nullable TGUser getTargetUser();

    /**
     * Stable per-instance UUID of the backend currently hosting the target.
     * Equals this server's instance id when {@link #isCrossServer()} is {@code false}.
     */
    @NotNull UUID getTargetServerInstanceId();

    /**
     * TotemGuard's friendly identity for the server hosting the target.
     */
    @NotNull String getTargetServerName();

    /**
     * {@code true} when the target is on a different backend than the caller
     * and the teleport requires a server hop.
     */
    boolean isCrossServer();
}
