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

import org.jetbrains.annotations.NotNull;

/**
 * Fired at the start of a TotemGuard shutdown, before any internal teardown
 * begins. Consumers receive this synchronously and should drop any cached API
 * references. The singleton returned by
 * {@link com.deathmotion.totemguard.api.TotemGuard#get()} becomes unavailable
 * shortly after this event resolves.
 * <p>
 * When the {@link Reason} is {@link Reason#LOADER_RESTART} or
 * {@link Reason#UPDATE_TRIGGERED}, TotemGuard intends to come back online
 * shortly. Consumers can wait for the API to become available again via
 * {@link com.deathmotion.totemguard.api.TotemGuard#getAsync()}.
 */
public interface TGPluginShutdownEvent extends TGEvent {

    @NotNull Reason getReason();

    @NotNull String getVersion();

    enum Reason {
        /**
         * The host platform (Paper or Fabric) is stopping or unloading the plugin.
         */
        SERVER_STOP,
        /**
         * The loader's {@code /tgloader restart} command is restarting the plugin.
         */
        LOADER_RESTART,
        /**
         * The loader is stopping the plugin without bringing it back online (for
         * example {@code /tgloader stop} or {@code /totemguard shutdown} on a
         * loader-managed install). Operators can re-enable with
         * {@code /tgloader start}.
         */
        LOADER_STOP,
        /**
         * An operator stopped the plugin via {@code /totemguard shutdown} on a
         * standalone install (no loader). It will not come back online until
         * the host platform is restarted.
         */
        OPERATOR_SHUTDOWN,
        /**
         * An update was downloaded and the loader is swapping the plugin jar.
         */
        UPDATE_TRIGGERED,
        /**
         * A startup or runtime error forced the plugin to abort.
         */
        ERROR
    }
}
