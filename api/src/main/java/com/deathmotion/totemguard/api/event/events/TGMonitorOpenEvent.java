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
 * Fired when a staff member opens the TotemGuard monitor GUI on another
 * player, and re-fired whenever a monitored target migrates to a different
 * backend in the fleet.
 * <p>
 * Cancelling blocks the open (or, on a re-fire, closes the already-open GUI
 * for that viewer).
 */
public interface TGMonitorOpenEvent extends TGEvent, Cancellable {

    @NotNull UUID getViewerUuid();

    @NotNull UUID getTargetUuid();

    @NotNull String getTargetName();

    /**
     * The target's {@link TGUser} handle when online on this server. {@code null} for cross-server targets.
     */
    @Nullable TGUser getTargetUser();

    @NotNull UUID getTargetServerInstanceId();

    @NotNull String getTargetServerName();

    /**
     * {@code true} when the target is on a different backend than the viewer.
     */
    boolean isCrossServer();

    /**
     * {@code true} when this event is being re-fired because the target moved
     * to a different backend while the viewer's monitor GUI was already open.
     */
    boolean isServerSwitch();
}
