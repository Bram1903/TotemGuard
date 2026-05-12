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
 * Fired when a player starts following another player via the TotemGuard
 * follow command, and re-fired whenever the followed target migrates to a
 * different backend in the fleet.
 * <p>
 * Cancelling the event blocks the follow from starting (or, on a re-fire,
 * stops an already-active follow for that follower). Plugins can use this
 * to forbid follow on specific servers or against specific targets — for
 * example, suppressing follow into a PvP arena.
 */
public interface TGFollowEvent extends Event, Cancellable {

    /**
     * UUID of the player who is following (or already following).
     */
    @NotNull UUID getFollowerUuid();

    /**
     * UUID of the player being followed.
     */
    @NotNull UUID getTargetUuid();

    /**
     * Display name of the player being followed, as known to the network at
     * dispatch time.
     */
    @NotNull String getTargetName();

    /**
     * The target's {@link TGUser} handle when they are online on this server.
     * {@code null} for cross-server targets.
     */
    @Nullable TGUser getTargetUser();

    /**
     * Stable per-instance UUID of the backend currently hosting the target.
     */
    @NotNull UUID getTargetServerInstanceId();

    /**
     * TotemGuard's friendly identity for the server hosting the target —
     * the name configured in {@code config.yml} and shown in alerts and
     * GUIs. Not necessarily the proxy's route name.
     */
    @NotNull String getTargetServerName();

    /**
     * {@code true} when the target is on a different backend than the follower.
     */
    boolean isCrossServer();

    /**
     * {@code true} when this event is being re-fired because the target
     * moved to a different backend while the follower was already actively
     * following. {@code false} on the initial start from the command.
     */
    boolean isServerSwitch();

    @Override
    boolean isCancelled();

    @Override
    void setCancelled(boolean cancelled);
}
