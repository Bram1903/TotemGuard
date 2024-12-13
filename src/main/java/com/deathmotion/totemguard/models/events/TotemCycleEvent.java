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

package com.deathmotion.totemguard.models.events;

import com.deathmotion.totemguard.models.TotemPlayer;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered after a full totem cycle.
 * A totem cycle occurs when a player pops a totem to prevent death,
 * and then moves a new totem into their off-hand slot.
 */
@Getter
public class TotemCycleEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    /**
     * The player associated with this totem cycle event.
     */
    private final Player player;

    /**
     * The TotemPlayer instance representing the player's totem state.
     */
    private final TotemPlayer totemPlayer;

    /**
     * Indicates whether the event has been canceled.
     */
    private boolean cancelled;

    /**
     * Constructs a new TotemCycleEvent.
     *
     * @param player      the player involved in this event
     * @param totemPlayer the totem-related data for the player
     */
    public TotemCycleEvent(Player player, TotemPlayer totemPlayer) {
        super(true);
        this.player = player;
        this.totemPlayer = totemPlayer;
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
     * @return true if the event is canceled, otherwise false
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether this event should be canceled.
     *
     * @param cancel true to cancel the event, otherwise false
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