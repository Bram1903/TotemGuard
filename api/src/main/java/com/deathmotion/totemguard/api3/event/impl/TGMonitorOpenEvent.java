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

package com.deathmotion.totemguard.api3.event.impl;

import com.deathmotion.totemguard.api3.event.Cancellable;
import com.deathmotion.totemguard.api3.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired when a staff member opens the TotemGuard monitor GUI on another player.
 * <p>
 * The {@linkplain #getViewerUuid() caller} is the viewer opening the monitor;
 * the {@linkplain #getTargetUuid() target} is the player whose inventory will be
 * displayed. Cancelling the event prevents the GUI from opening, which lets
 * servers gate inventory monitoring on staff state (e.g., block during events, or for specific targets).
 */
public interface TGMonitorOpenEvent extends Event, Cancellable {

    /**
     * The UUID of the staff member opening the monitor.
     */
    @NotNull UUID getViewerUuid();

    /**
     * The UUID of the player about to be monitored.
     */
    @NotNull UUID getTargetUuid();

    @Override
    boolean isCancelled();

    @Override
    void setCancelled(boolean cancelled);
}
