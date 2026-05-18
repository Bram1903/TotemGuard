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
 * TotemGuard's event bus. The only entry point for plugin event subscriptions.
 *
 * <h2>Subscribing</h2>
 * Look up the channel by the event class and subscribe a handler:
 * <pre>{@code
 * EventBus bus = TotemGuard.get().getEventBus();
 *
 * bus.get(TGUserJoinEvent.class).subscribe(this, event ->
 *     getLogger().info(event.getUser().getName() + " joined"));
 *
 * bus.get(TGUserFlagEvent.class).subscribe(this, event -> {
 *     if (whitelisted.contains(event.getUser().getUuid())) event.setCancelled(true);
 * });
 * }</pre>
 *
 * <h2>Abstract subscribe</h2>
 * Subscribing to a supertype interface (for example {@link com.deathmotion.totemguard.api.event.events.TGCheckEvent}
 * or {@link com.deathmotion.totemguard.api.event.events.TGUserEvent}) fans out
 * to every concrete event that implements it. The handler is invoked once per
 * matching dispatch.
 * <pre>{@code
 * bus.get(TGCheckEvent.class).subscribe(this, event -> {
 *     // fires for TGUserFlagEvent and TGUserPunishEvent
 * });
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * Every subscribe accepts a plugin context object which the bus remembers
 * alongside the handler. From {@code onDisable} (or whatever the platform
 * equivalent is) call {@link #unregisterAll(Object)} with that same context
 * and every handler the plugin registered drops off in one call.
 * <p>
 * On Paper, when the plugin context passed to subscribe is the
 * {@code JavaPlugin} instance itself, TotemGuard also clears the plugin's
 * handlers automatically when Bukkit fires {@code PluginDisableEvent}.
 * Calling {@link #unregisterAll(Object)} from {@code onDisable} is still
 * recommended for portability with other platforms.
 */
public interface EventBus {

    /**
     * Resolves the channel for an event class. Works for any event interface
     * TotemGuard publishes, including supertypes such as
     * {@link com.deathmotion.totemguard.api.event.events.TGCheckEvent} (which
     * routes to every concrete subtype) and the
     * {@link com.deathmotion.totemguard.api.event.events.TGUserFlagEvent}
     * concrete events themselves.
     * <p>
     * Channel instances live as long as the bus does. Caching them in a
     * {@code static final} field is fine if subscribe latency matters.
     *
     * @param eventClass the event interface to look up
     * @return the channel, never {@code null}
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
     * Drops every handler that was registered with the given plugin context,
     * across all channels at once. Use this from a plugin's shutdown hook so
     * stale handlers from a previous load do not stay alive on the bus.
     */
    void unregisterAll(@NotNull Object pluginContext);
}
