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
 * Handles subscriptions for one specific event class. Plugins acquire one via
 * {@link EventBus#get(Class)}, then register handlers through it.
 * <p>
 * The same channel object is reused for the lifetime of the bus, so callers
 * that subscribe from a hot path are welcome to stash a reference somewhere.
 * Only this interface is exposed. The dispatch machinery itself lives inside
 * the common module.
 *
 * @param <E> the event interface this channel hands out
 */
public interface EventChannel<E> {

    /**
     * Returns the event interface that this channel routes.
     */
    @NotNull Class<E> eventType();

    /**
     * Registers a handler at {@link EventPriority#NORMAL}. Handlers registered
     * with this overload run regardless of whether a higher-priority handler
     * has already cancelled the event.
     *
     * @param pluginContext owner reference used for lifecycle cleanup. From a
     *                      Bukkit {@code JavaPlugin} or Fabric
     *                      {@code ModInitializer}, pass {@code this}.
     *                      {@link EventBus#unregisterAll(Object)} compares
     *                      this value by reference identity.
     * @param handler       invoked once per fired event
     * @return a handle that can detach this exact registration
     */
    @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler);

    /**
     * Registers a handler at the given priority slot. See
     * {@link EventPriority} for the recommended slot values.
     */
    @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler, int priority);

    /**
     * Registers a handler at the given priority, with control over whether it
     * still runs for events that were already cancelled by an earlier
     * handler. Passing {@code true} for non-cancellable events has no effect.
     */
    @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler,
                                    int priority, boolean ignoreCancelled);

    /**
     * Detaches a specific handler by reference identity. Does nothing when
     * the handler is not currently registered on this channel.
     */
    void unsubscribe(@NotNull Consumer<? super E> handler);

    /**
     * Reports whether this channel currently has any handlers attached.
     * Useful as a cheap gate around expensive event-construction work.
     */
    boolean isEmpty();
}
