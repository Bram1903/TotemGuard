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

package com.deathmotion.totemguard.common.event;

import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.api.event.EventOrder;
import com.deathmotion.totemguard.api.event.EventRepository;
import com.deathmotion.totemguard.api.event.EventSubscription;
import com.deathmotion.totemguard.common.event.internal.InternalEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventRepositoryImpl implements EventRepository {

    private final Map<Class<?>, Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>>> listeners = new ConcurrentHashMap<>();
    private final Map<Key, Consumer<? super Event>> boxedByKey = new ConcurrentHashMap<>();

    @Override
    public <T extends Event> @NotNull EventSubscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        final Key key = new Key(eventType, order, listener);

        final Consumer<? super Event> boxed = boxedByKey.computeIfAbsent(
                key,
                k -> (Event e) -> listener.accept(eventType.cast(e))
        );

        Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>> perType =
                listeners.computeIfAbsent(eventType, k -> new EnumMap<>(EventOrder.class));

        CopyOnWriteArrayList<Consumer<? super Event>> bucket =
                perType.computeIfAbsent(order, k -> new CopyOnWriteArrayList<>());

        if (!bucket.contains(boxed)) bucket.add(boxed);

        return () -> {
            boolean removed = bucket.remove(boxed);
            if (removed) {
                boxedByKey.remove(key, boxed);
            }
        };
    }

    @Override
    public @NotNull EventSubscription subscribeAll(
            @NotNull EventOrder order,
            @NotNull Consumer<? super Event> listener
    ) {
        final Key key = new Key(Event.class, order, listener);
        final Consumer<? super Event> boxed = boxedByKey.computeIfAbsent(key, k -> listener);

        Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>> perType =
                listeners.computeIfAbsent(Event.class, k -> new EnumMap<>(EventOrder.class));

        CopyOnWriteArrayList<Consumer<? super Event>> bucket =
                perType.computeIfAbsent(order, k -> new CopyOnWriteArrayList<>());

        if (!bucket.contains(boxed)) bucket.add(boxed);

        return () -> {
            boolean removed = bucket.remove(boxed);
            if (removed) boxedByKey.remove(key, boxed);
        };
    }

    public @NotNull EventSubscription subscribeAllIncludingInternal(
            @NotNull Consumer<? super Event> listener
    ) {
        return subscribeAllIncludingInternal(EventOrder.NORMAL, listener);
    }

    public @NotNull EventSubscription subscribeAllIncludingInternal(
            @NotNull EventOrder order,
            @NotNull Consumer<? super Event> listener
    ) {
        final Key key = new Key(AnyIncludingInternal.class, order, listener);
        final Consumer<? super Event> boxed = boxedByKey.computeIfAbsent(key, k -> listener);

        Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>> perType = listeners.computeIfAbsent(AnyIncludingInternal.class, k -> new EnumMap<>(EventOrder.class));

        CopyOnWriteArrayList<Consumer<? super Event>> bucket =
                perType.computeIfAbsent(order, k -> new CopyOnWriteArrayList<>());

        if (!bucket.contains(boxed)) bucket.add(boxed);

        return () -> {
            boolean removed = bucket.remove(boxed);
            if (removed) boxedByKey.remove(key, boxed);
        };
    }


    @Override
    public <T extends Event> boolean unsubscribe(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        final Key key = new Key(eventType, order, listener);
        final Consumer<? super Event> boxed = boxedByKey.remove(key);
        if (boxed == null) return false;

        Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>> perType = listeners.get(eventType);
        if (perType == null) return false;

        CopyOnWriteArrayList<Consumer<? super Event>> bucket = perType.get(order);
        if (bucket == null) return false;

        return bucket.remove(boxed);
    }

    @Override
    public boolean unsubscribeAll(
            @NotNull EventOrder order,
            @NotNull Consumer<? super Event> listener
    ) {
        final Key key = new Key(Event.class, order, listener);
        final Consumer<? super Event> boxed = boxedByKey.remove(key);
        if (boxed == null) return false;

        Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>> perType = listeners.get(Event.class);
        if (perType == null) return false;

        CopyOnWriteArrayList<Consumer<? super Event>> bucket = perType.get(order);
        if (bucket == null) return false;

        return bucket.remove(boxed);
    }

    public @NotNull Event post(@NotNull Event event) {
        final boolean internal = event instanceof InternalEvent;

        for (EventOrder order : EventOrder.values()) {
            dispatchBucketFor(event.getClass(), order, event);

            dispatchBucketFor(AnyIncludingInternal.class, order, event);

            // Prevent internal events from reaching other plugins' global (any-event) listeners
            if (!internal) {
                dispatchBucketFor(Event.class, order, event);
            }
        }
        return event;
    }

    private void dispatchBucketFor(
            Class<?> key,
            EventOrder order,
            Event event
    ) {
        Map<EventOrder, CopyOnWriteArrayList<Consumer<? super Event>>> perType = listeners.get(key);
        if (perType == null) return;

        CopyOnWriteArrayList<Consumer<? super Event>> bucket = perType.get(order);
        if (bucket == null || bucket.isEmpty()) return;

        for (Consumer<? super Event> l : bucket) {
            l.accept(event);
        }
    }

    private static final class AnyIncludingInternal {
    }

    private record Key(Class<?> type, EventOrder order, Consumer<?> original) {
        private Key(Class<?> type, EventOrder order, Consumer<?> original) {
            this.type = Objects.requireNonNull(type, "type");
            this.order = Objects.requireNonNull(order, "order");
            this.original = Objects.requireNonNull(original, "original");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return type.equals(key.type) && order == key.order && original == key.original;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + order.hashCode();
            result = 31 * result + System.identityHashCode(original);
            return result;
        }
    }
}
