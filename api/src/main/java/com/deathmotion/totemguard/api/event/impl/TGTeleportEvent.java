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
import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when a staff member runs {@code /tg teleport <player>}.
 * <p>
 * The caller is identified by UUID only, since admins running this command
 * typically hold {@code TotemGuard.Bypass} and are therefore not registered
 * as a {@link TGUser}. The teleport target, however, is always a tracked
 * player on some backend in the fleet; if the target is online on this
 * server, {@link #getTargetUser()} returns its {@code TGUser} handle.
 * <p>
 * Cancelling aborts the teleport before either the local hop or the
 * cross-server route is published. Useful for combat-state gates or
 * per-target permission checks.
 */
public interface TGTeleportEvent extends Event, Cancellable {

    /**
     * UUID of the staff member running {@code /tg teleport}.
     */
    @NotNull UUID getCallerUuid();

    /**
     * UUID of the teleport target.
     */
    @NotNull UUID getTargetUuid();

    /**
     * Display name of the teleport target as known to the network at
     * dispatch time.
     */
    @NotNull String getTargetName();

    /**
     * The target's {@link TGUser} handle when they are online on this server.
     * {@code null} for cross-server targets, since {@code TGUser} is local-only.
     */
    @Nullable TGUser getTargetUser();

    /**
     * Stable per-instance UUID of the backend currently hosting the target.
     * Generated at backend startup; persists for that server's lifetime.
     * Equals this server's instance id when {@link #isCrossServer()} is {@code false}.
     */
    @NotNull UUID getTargetServerInstanceId();

    /**
     * TotemGuard's friendly identity for the server hosting the target —
     * the name configured in {@code config.yml} and shown in alerts and
     * GUIs. This is <em>not</em> guaranteed to match the proxy's route
     * name; cross-server teleports translate it through
     * {@code velocity.toml} / BungeeCord casing fixes before publishing.
     */
    @NotNull String getTargetServerName();

    /**
     * The proxy-side route id for the server hosting the target — the
     * value the proxy uses to send a player to that backend (e.g. the
     * key in {@code velocity.toml}'s {@code servers} block).
     * <p>
     * {@code null} when the local backend is not behind a proxy, or when
     * the proxy hasn't yet replied with its server list, or when the
     * friendly server name has no entry in the proxy's config. Consumers
     * relying on this for routing should treat {@code null} as "I cannot
     * forward this player".
     */
    @Nullable String getTargetProxyServerId();

    /**
     * {@code true} when the target is on a different backend than the caller
     * and the teleport requires a server hop.
     */
    boolean isCrossServer();

    @Override
    boolean isCancelled();

    @Override
    void setCancelled(boolean cancelled);
}
