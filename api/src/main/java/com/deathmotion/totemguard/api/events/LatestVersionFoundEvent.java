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

import com.deathmotion.totemguard.api.versioning.TGVersion;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered when the latest version of the TotemGuard plugin
 * is successfully retrieved.
 *
 * <p>This event does not necessarily indicate that an update is available.
 * Instead, it signifies that the latest version information has been fetched
 * from the GitHub API.</p>
 *
 * <p>Handlers for this event can access the latest version of the plugin
 * through the {@link #getLatestVersion()} method.</p>
 *
 * <p>Example use cases include notifying administrators or logging version
 * information for diagnostics.</p>
 */
@Getter
public class LatestVersionFoundEvent extends Event {

    /**
     * The handler list for this event type.
     */
    private static final HandlerList handlers = new HandlerList();

    /**
     * The latest version of the plugin that has been found.
     */
    private final TGVersion latestVersion;

    /**
     * Constructs a new {@code UpdateFoundEvent}.
     *
     * @param latestVersion the latest version of the plugin that has been found
     */
    public LatestVersionFoundEvent(TGVersion latestVersion) {
        super(true);
        this.latestVersion = latestVersion;
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
     * Gets the handler list for this event instance.
     *
     * @return the handler list
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
