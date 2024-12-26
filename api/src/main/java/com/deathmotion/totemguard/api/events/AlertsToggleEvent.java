/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered when a player toggles alerts.
 *
 * <p>This event provides detailed information about the player and the state
 * of the alerts. It is cancellable, allowing other plugins to prevent the
 * player from toggling alerts and handle the situation differently.</p>
 *
 * <p>Example use cases include:
 * <ul>
 *   <li>Customizing alert toggling based on the player's status or context.</li>
 *   <li>Preventing players from disabling alerts in certain situations.</li>
 *   <li>Logging detailed information about alert toggling events.</li>
 * </ul>
 * </p>
 */
@Getter
public class AlertsToggleEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final boolean enabled;

    private boolean cancelled;

    /**
     * Constructs a new {@code AlertsToggleEvent}.
     *
     * @param player  the player toggling alerts
     * @param enabled {@code true} if alerts are being enabled; {@code false} if they are being disabled
     */
    public AlertsToggleEvent(Player player, boolean enabled) {
        super(false);
        this.player = player;
        this.enabled = enabled;
    }

    /**
     * Gets the static handler list for this event type.
     *
     * @return the static handler list
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Checks whether the event is canceled.
     *
     * @return {@code true} if the event is canceled; {@code false} otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the event should be canceled.
     *
     * @param cancel {@code true} to cancel the event; {@code false} otherwise
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the list of handlers for this event instance.
     *
     * @return the handler list
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
