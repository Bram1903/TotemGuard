/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.api.events;

import com.deathmotion.totemguard.api.interfaces.IAbstractCheck;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered when a check flags a player.
 * This event is cancellable, allowing other plugins to prevent actions based on the flag.
 */
@Getter
public class FlagEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    /**
     * The player who triggered the flag.
     */
    private Player player;

    /**
     * The check that triggered the flag.
     */
    private IAbstractCheck check;

    /**
     * Whether the event has been cancelled.
     */
    private boolean cancelled;

    /**
     * Constructs a new {@code FlagEvent}.
     *
     * @param player the player who triggered the flag
     * @param check  the check that triggered the flag
     */
    public FlagEvent(Player player, IAbstractCheck check) {
        super(true);
        this.player = player;
        this.check = check;
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
     * Sets whether the event should be cancelled.
     *
     * @param cancel {@code true} to cancel the event; {@code false} otherwise
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return the handler list
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}