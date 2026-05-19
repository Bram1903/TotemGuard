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
 * Subscription channel for one event class. Acquired via {@link EventBus#get(Class)}.
 * The same channel is reused for the bus's lifetime and is safe to cache.
 *
 * @param <E> the event interface this channel routes
 */
public interface EventChannel<E> {

    /**
     * The event interface this channel routes, the same class passed to
     * {@link EventBus#get(Class)}. Useful for generic plumbing that holds channels by erasure.
     */
    @NotNull Class<E> eventType();

    /**
     * Registers a handler at {@link EventPriority#NORMAL}. Runs regardless of prior
     * cancellation. {@code pluginContext} is the key for
     * {@link EventBus#unregisterAll(Object)} (compared by reference identity).
     */
    @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler);

    /**
     * Registers a handler at an explicit priority slot. Lower values run earlier, higher
     * values run later, see {@link EventPriority} for the shared vocabulary. Handlers
     * still run regardless of prior cancellation, use the four-arg overload for
     * cancellation gating.
     */
    @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler, int priority);

    /**
     * Registers a handler with a priority and an option to skip already-cancelled events.
     * {@code ignoreCancelled} has no effect on non-cancellable events.
     */
    @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler,
                                    int priority, boolean ignoreCancelled);

    /**
     * Detaches a handler by reference identity. No-op if not registered.
     */
    void unsubscribe(@NotNull Consumer<? super E> handler);

    /**
     * Whether this channel has any handlers. Useful as a gate around expensive event construction.
     */
    boolean isEmpty();
}
