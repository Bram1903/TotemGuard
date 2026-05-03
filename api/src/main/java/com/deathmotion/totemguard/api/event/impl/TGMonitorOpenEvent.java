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
 * Fired when a staff member opens the TotemGuard monitor GUI on another
 * player, and re-fired whenever a monitored target migrates to a different
 * backend in the fleet.
 * <p>
 * Cancelling the event blocks the open (or, on a re-fire, closes the
 * already-open GUI for that viewer). Plugins can use this to forbid
 * monitoring on specific servers — for example, suppressing inventory
 * monitoring while the target is on a PvP arena.
 */
public interface TGMonitorOpenEvent extends Event, Cancellable {

    /**
     * UUID of the staff member opening (or already viewing) the monitor.
     */
    @NotNull UUID getViewerUuid();

    /**
     * UUID of the player being monitored.
     */
    @NotNull UUID getTargetUuid();

    /**
     * Display name of the player being monitored, as known to the network at
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
     * The proxy-side route id for the server hosting the target — the
     * value the proxy uses to send a player to that backend. {@code null}
     * when no proxy is attached, or when the friendly server name has no
     * entry in the proxy's config.
     */
    @Nullable String getTargetProxyServerId();

    /**
     * {@code true} when the target is on a different backend than the viewer.
     */
    boolean isCrossServer();

    /**
     * {@code true} when this event is being re-fired because the target
     * moved to a different backend while the viewer's monitor GUI was
     * already open. {@code false} on the initial open from the command.
     */
    boolean isServerSwitch();

    @Override
    boolean isCancelled();

    @Override
    void setCancelled(boolean cancelled);
}
