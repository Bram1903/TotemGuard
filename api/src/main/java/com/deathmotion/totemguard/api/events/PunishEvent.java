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

import com.deathmotion.totemguard.api.interfaces.AbstractCheck;
import com.deathmotion.totemguard.api.interfaces.TotemUser;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered when a player is about to be punished by a TotemGuard check.
 *
 * <p>This event provides detailed information about the player and the check
 * that triggered the punishment. It is cancellable, allowing other plugins to
 * prevent the punishment and handle the situation differently.</p>
 *
 * <p>Example use cases include:
 * <ul>
 *   <li>Customizing punishment actions based on the player's status or context.</li>
 *   <li>Preventing punishment for legitimate players under specific conditions.</li>
 *   <li>Logging detailed information about punishment events.</li>
 * </ul>
 * </p>
 */
@Getter
public class PunishEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final TotemUser totemUser;
    private final AbstractCheck check;

    private boolean cancelled;

    /**
     * Constructs a new {@code PunishEvent}.
     *
     * @param totemUser the player who is being punished
     * @param check     the details of the check that triggered the punishment
     */
    public PunishEvent(TotemUser totemUser, AbstractCheck check) {
        super(true);
        this.totemUser = totemUser;
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
     * Checks whether this event is canceled.
     *
     * @return {@code true} if the event is canceled; {@code false} otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether this event should be canceled.
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