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

import com.deathmotion.totemguard.api.interfaces.ICheckDetails;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that is triggered when a player is about to be punished
 * by a TotemGuard check. This event can be canceled to prevent the punishment.
 */
@Getter
public class PunishEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The player who is being punished.
     */
    private final Player player;

    /**
     * The details of the check that triggered this punishment.
     */
    private final ICheckDetails checkDetails;

    /**
     * Whether the event is canceled.
     */
    private boolean cancelled;

    /**
     * Constructs a new PunishEvent.
     *
     * @param player       the player who is being punished
     * @param checkDetails the details of the check that triggered the punishment
     */
    public PunishEvent(Player player, ICheckDetails checkDetails) {
        super(true);
        this.player = player;
        this.checkDetails = checkDetails;
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
     * Gets whether this event is canceled.
     *
     * @return {@code true} if the event is canceled, otherwise {@code false}
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether this event should be canceled.
     *
     * @param cancel {@code true} to cancel the event, otherwise {@code false}
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the handler list for this event instance.
     *
     * @return the handler list
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}