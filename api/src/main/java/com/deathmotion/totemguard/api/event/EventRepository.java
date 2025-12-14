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

package com.deathmotion.totemguard.api.event;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Manages subscription and unsubscription of {@link Event} listeners.
 */
public interface EventRepository {

    /**
     * Subscribes a listener to a specific event type with the given order.
     *
     * @param eventType the event class to listen for
     * @param order     the invocation order
     * @param listener  the event consumer
     * @param <T>       the event type
     * @return a subscription handle used to unsubscribe
     */
    <T extends Event> @NotNull EventSubscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    );

    /**
     * Subscribes a listener to a specific event type using {@link EventOrder#NORMAL}.
     *
     * @param eventType the event class to listen for
     * @param listener  the event consumer
     * @param <T>       the event type
     * @return a subscription handle used to unsubscribe
     */
    default <T extends Event> @NotNull EventSubscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<? super T> listener
    ) {
        return subscribe(eventType, EventOrder.NORMAL, listener);
    }

    /**
     * Subscribes a listener to all events with the given order.
     *
     * @param order    the invocation order
     * @param listener the event consumer
     * @return a subscription handle used to unsubscribe
     */
    @NotNull EventSubscription subscribeAll(
            @NotNull EventOrder order,
            @NotNull Consumer<? super Event> listener
    );

    /**
     * Subscribes a listener to all events using {@link EventOrder#NORMAL}.
     *
     * @param listener the event consumer
     * @return a subscription handle used to unsubscribe
     */
    default @NotNull EventSubscription subscribeAll(
            @NotNull Consumer<? super Event> listener
    ) {
        return subscribeAll(EventOrder.NORMAL, listener);
    }

    /**
     * Unsubscribes a listener from a specific event type and order.
     *
     * @param eventType the event class
     * @param order     the invocation order
     * @param listener  the event consumer
     * @param <T>       the event type
     * @return true if the listener was removed
     */
    <T extends Event> boolean unsubscribe(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    );

    /**
     * Unsubscribes a listener from a specific event type using {@link EventOrder#NORMAL}.
     *
     * @param eventType the event class
     * @param listener  the event consumer
     * @param <T>       the event type
     * @return true if the listener was removed
     */
    default <T extends Event> boolean unsubscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<? super T> listener
    ) {
        return unsubscribe(eventType, EventOrder.NORMAL, listener);
    }

    /**
     * Unsubscribes a listener from all events for the given order.
     *
     * @param order    the invocation order
     * @param listener the event consumer
     * @return true if the listener was removed
     */
    boolean unsubscribeAll(
            @NotNull EventOrder order,
            @NotNull Consumer<? super Event> listener
    );

    /**
     * Unsubscribes a listener from all events using {@link EventOrder#NORMAL}.
     *
     * @param listener the event consumer
     * @return true if the listener was removed
     */
    default boolean unsubscribeAll(
            @NotNull Consumer<? super Event> listener
    ) {
        return unsubscribeAll(EventOrder.NORMAL, listener);
    }
}
