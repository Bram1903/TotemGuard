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

package com.deathmotion.totemguard.api.event;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * TotemGuard's event bus, the only entry point for plugin event subscriptions.
 * Subscribing to a supertype event interface (such as
 * {@link com.deathmotion.totemguard.api.event.events.TGCheckEvent} or
 * {@link com.deathmotion.totemguard.api.event.events.TGUserEvent}) fans out to every
 * concrete subtype. The plugin context passed to subscribe is the key for
 * {@link #unregisterAll(Object)}, which should be called from the plugin's shutdown
 * hook. On Paper, when the context is the {@code JavaPlugin} itself, handlers are
 * also cleared automatically on {@code PluginDisableEvent}.
 */
public interface EventBus {

    /**
     * Resolves the channel for an event class, including supertype events that fan out
     * to concrete subtypes. Channels live for the lifetime of the bus and may be cached.
     *
     * @throws IllegalArgumentException if no event matches the class
     */
    <E> @NotNull EventChannel<E> get(@NotNull Class<E> eventClass);

    /**
     * Shorthand for {@code get(eventClass).subscribe(pluginContext, handler)}.
     */
    default <E> @NotNull Subscription subscribe(@NotNull Class<E> eventClass,
                                                @NotNull Object pluginContext,
                                                @NotNull Consumer<? super E> handler) {
        return get(eventClass).subscribe(pluginContext, handler);
    }

    /**
     * Drops every handler registered with the given plugin context across all channels.
     * Compared by reference identity.
     */
    void unregisterAll(@NotNull Object pluginContext);
}
