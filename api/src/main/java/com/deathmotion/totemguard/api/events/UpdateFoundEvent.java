/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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
 * Represents an event triggered when a new update for the TotemGuard plugin is found.
 *
 * <p>This event indicates that a newer version of the plugin is available compared
 * to the currently installed version. It provides access to the details of the
 * latest version retrieved from the GitHub API.</p>
 *
 * <p>Example use cases include:
 * <ul>
 *   <li>Notifying server administrators about the availability of a new update.</li>
 *   <li>Logging update information for plugin management and audits.</li>
 *   <li>Triggering custom update reminders or announcements.</li>
 * </ul>
 * </p>
 *
 * <p>Note: This event will not be triggered if the installed version of the plugin
 * is already up-to-date.</p>
 */
@Getter
public class UpdateFoundEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final TGVersion latestVersion;

    /**
     * Constructs a new {@code UpdateFoundEvent}.
     *
     * @param latestVersion the newer version of the plugin that has been found
     */
    public UpdateFoundEvent(TGVersion latestVersion) {
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