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
 * Fired at the start of a TotemGuard shutdown, synchronously, before any internal
 * teardown. Consumers should drop cached API references, the singleton from
 * {@link com.deathmotion.totemguard.api.TotemGuard#get()} becomes unavailable shortly
 * after. For {@link Reason#LOADER_RESTART} and {@link Reason#UPDATE_TRIGGERED} the
 * plugin will come back, consumers can re-acquire via
 * {@link com.deathmotion.totemguard.api.TotemGuard#getAsync()}.
 */
public interface TGPluginShutdownEvent extends TGEvent {

    /**
     * Cause of the shutdown, lets handlers decide whether the API will be coming back
     * ({@link Reason#LOADER_RESTART}, {@link Reason#UPDATE_TRIGGERED}) or is gone for good
     * ({@link Reason#SERVER_STOP}, {@link Reason#LOADER_STOP}, {@link Reason#OPERATOR_SHUTDOWN},
     * {@link Reason#ERROR}).
     */
    @NotNull Reason getReason();

    /**
     * Version string of the plugin that is shutting down (the value reported by
     * {@link com.deathmotion.totemguard.api.TotemGuardAPI#getVersion()}). Captured before
     * teardown so it remains accurate inside the handler.
     */
    @NotNull String getVersion();

    /**
     * Distinguishes the cause of a {@link TGPluginShutdownEvent}, see each constant for semantics.
     */
    enum Reason {
        /**
         * Host platform (Paper or Fabric) is stopping or unloading the plugin.
         */
        SERVER_STOP,
        /**
         * Loader's {@code /tgloader restart} is restarting the plugin.
         */
        LOADER_RESTART,
        /**
         * Loader stopped the plugin without bringing it back online (e.g. {@code /tgloader stop}).
         * Operators re-enable with {@code /tgloader start}.
         */
        LOADER_STOP,
        /**
         * Operator stopped the plugin via {@code /totemguard shutdown} on a standalone install.
         * Stays down until the host restarts.
         */
        OPERATOR_SHUTDOWN,
        /**
         * An update was downloaded and the loader is swapping the plugin jar.
         */
        UPDATE_TRIGGERED,
        /**
         * Startup or runtime error forced the plugin to abort.
         */
        ERROR
    }
}
