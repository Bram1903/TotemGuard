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

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered when the TotemGuard API is enabled.
 *
 * <p>This event is called during the initialization process of the TotemGuard API
 * and signals that the API is now available for use. Handlers of this event
 * can perform setup operations or register features dependent on the API.</p>
 */
public class ApiEnabledEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    /**
     * Constructs a new {@code ApiEnabledEvent}.
     */
    public ApiEnabledEvent() {
        super(true);
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
     * Gets the list of handlers for this event instance.
     *
     * @return the handler list
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
