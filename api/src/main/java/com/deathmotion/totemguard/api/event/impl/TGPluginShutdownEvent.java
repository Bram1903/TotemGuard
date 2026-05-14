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

package com.deathmotion.totemguard.api.event.impl;

import com.deathmotion.totemguard.api.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Fired at the start of a TotemGuard shutdown, before any internal teardown begins.
 * Consumers receive this synchronously and should drop any cached API references;
 * the singleton returned by {@link com.deathmotion.totemguard.api.TotemGuard#get()}
 * becomes unavailable shortly after this event resolves.
 * <p>
 * When the {@code Reason} is {@link Reason#LOADER_RESTART} or
 * {@link Reason#UPDATE_TRIGGERED}, TotemGuard intends to come back online shortly;
 * consumers can wait for the API to become available again via
 * {@link com.deathmotion.totemguard.api.TotemGuard#getAsync()}.
 */
public final class TGPluginShutdownEvent implements Event {

    private final long timestamp;
    private final Reason reason;
    private final String version;

    public TGPluginShutdownEvent(@NotNull Reason reason, @NotNull String version) {
        this.reason = Objects.requireNonNull(reason, "reason");
        this.version = Objects.requireNonNull(version, "version");
        this.timestamp = System.currentTimeMillis();
    }

    public @NotNull Reason getReason() {
        return reason;
    }

    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Why TotemGuard is shutting down.
     */
    public enum Reason {
        /**
         * The host platform (Bukkit/Fabric) is stopping or unloading the plugin.
         */
        SERVER_STOP,
        /**
         * The loader's {@code /tgloader restart} command is restarting the inner plugin.
         */
        LOADER_RESTART,
        /**
         * An update was downloaded and the loader is swapping the inner plugin.
         */
        UPDATE_TRIGGERED,
        /**
         * A startup or runtime error forced the plugin to abort.
         */
        ERROR
    }
}
