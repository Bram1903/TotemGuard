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
 * Fired when a staff member toggles {@code /tg focus}, and re-fired when a
 * cached focus is restored on join (e.g. after a server hop).
 * <p>
 * The caller is identified by UUID only — admins running this command
 * typically hold {@code TotemGuard.Bypass} and are therefore not registered
 * as a {@link TGUser}. Target fields describe the player being focused
 * and are populated only when {@link #isEnabling()} is {@code true}; on a
 * disable they are all {@code null}.
 * <p>
 * Cancelling on enable blocks the focus from being set. Cancelling on
 * disable keeps the existing focus active.
 */
public interface TGFocusEvent extends Event, Cancellable {

    /**
     * UUID of the staff member running the command (or whose cached focus is
     * being restored on join).
     */
    @NotNull UUID getCallerUuid();

    /**
     * UUID of the focus target. {@code null} when {@link #isEnabling()} is
     * {@code false}.
     */
    @Nullable UUID getTargetUuid();

    /**
     * Display name of the focus target. {@code null} when {@link #isEnabling()}
     * is {@code false}.
     */
    @Nullable String getTargetName();

    /**
     * The target's {@link TGUser} handle when they are online on this server.
     * {@code null} for cross-server targets and on a disable call.
     */
    @Nullable TGUser getTargetUser();

    /**
     * Stable per-instance UUID of the backend currently hosting the target.
     * {@code null} on a disable call.
     */
    @Nullable UUID getTargetServerInstanceId();

    /**
     * TotemGuard's friendly identity for the server hosting the target —
     * the name configured in {@code config.yml} and shown in alerts and
     * GUIs. Not necessarily the proxy's route name. {@code null} on a
     * disable call.
     */
    @Nullable String getTargetServerName();

    /**
     * The proxy-side route id for the server hosting the target.
     * {@code null} on a disable call, or when no proxy is attached, or
     * when the friendly server name has no entry in the proxy's config.
     */
    @Nullable String getTargetProxyServerId();

    /**
     * {@code true} when the call sets a focus, {@code false} when it clears
     * an existing focus.
     */
    boolean isEnabling();

    /**
     * {@code true} when this event is being re-fired because a cached focus
     * is being restored on player join (typically after a cross-server hop).
     * {@code false} for an interactive {@code /tg focus} call.
     */
    boolean isRestore();

    @Override
    boolean isCancelled();

    @Override
    void setCancelled(boolean cancelled);
}
