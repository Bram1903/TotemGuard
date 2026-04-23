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

package com.deathmotion.totemguard.common.event;

import com.deathmotion.totemguard.api3.event.Event;
import com.deathmotion.totemguard.api3.event.EventOrder;
import com.deathmotion.totemguard.api3.event.EventRepository;
import com.deathmotion.totemguard.api3.event.EventSubscription;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.internal.InternalEvent;
import com.deathmotion.totemguard.common.util.SortedMerge;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

public class EventRepositoryImpl implements EventRepository {

    private static final EventOrder[] EVENT_ORDERS = EventOrder.values();
    private static final RegisteredListener[] NO_LISTENERS = new RegisteredListener[0];

    private final ListenerRegistry publicListeners = new ListenerRegistry();
    private final ListenerRegistry internalListeners = new ListenerRegistry();
    private final Map<Class<?>, Class<?>[]> dispatchTypes = new ConcurrentHashMap<>();
    private final Map<DispatchPlanKey, RegisteredListener[]> dispatchPlans = new ConcurrentHashMap<>();
    private final AtomicLong nextSequence = new AtomicLong();
    private final ThreadLocal<Integer> internalDispatchDepth = ThreadLocal.withInitial(() -> 0);

    @Override
    public <T extends Event> @NotNull EventSubscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        return subscribe(publicListeners, eventType, order, listener);
    }

    public <T extends Event> @NotNull EventSubscription subscribeInternal(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        return subscribe(internalListeners, eventType, order, listener);
    }

    public <T extends Event> @NotNull EventSubscription subscribeInternal(
            @NotNull Class<T> eventType,
            @NotNull Consumer<? super T> listener
    ) {
        return subscribeInternal(eventType, EventOrder.NORMAL, listener);
    }

    @Override
    public <T extends Event> boolean unsubscribe(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        return unsubscribe(publicListeners, eventType, order, listener);
    }

    public <T extends Event> boolean unsubscribeInternal(
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        return unsubscribe(internalListeners, eventType, order, listener);
    }

    public <T extends Event> boolean unsubscribeInternal(
            @NotNull Class<T> eventType,
            @NotNull Consumer<? super T> listener
    ) {
        return unsubscribeInternal(eventType, EventOrder.NORMAL, listener);
    }

    public <T extends Event> @NotNull T post(@NotNull T event) {
        Objects.requireNonNull(event, "event");
        final boolean internalOnly = shouldStayInternal(event);

        final RegisteredListener[] dispatchPlan = dispatchPlans.computeIfAbsent(
                new DispatchPlanKey(event.getClass(), internalOnly),
                this::buildDispatchPlan
        );

        if (internalOnly) {
            internalDispatchDepth.set(internalDispatchDepth.get() + 1);
        }

        try {
            for (RegisteredListener listener : dispatchPlan) {
                try {
                    listener.accept(event);
                } catch (Exception exception) {
                    TGPlatform.getInstance().getLogger().log(
                            Level.WARNING,
                            "Failed to dispatch event " + event.getClass().getName() + " to listener " + listener.listenerClassName(),
                            exception
                    );
                }
            }
        } finally {
            if (internalOnly) {
                exitInternalDispatch();
            }
        }
        return event;
    }

    private boolean shouldStayInternal(Event event) {
        return event instanceof InternalEvent || internalDispatchDepth.get() > 0;
    }

    private void exitInternalDispatch() {
        int remaining = internalDispatchDepth.get() - 1;
        if (remaining <= 0) internalDispatchDepth.remove();
        else internalDispatchDepth.set(remaining);
    }

    private <T extends Event> @NotNull EventSubscription subscribe(
            ListenerRegistry registry,
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        final Key key = new Key(eventType, order, listener);
        final RegisteredListener registration = registry.registrations.computeIfAbsent(
                key,
                ignored -> RegisteredListener.typed(eventType, listener, nextSequence.getAndIncrement())
        );

        if (registry
                .listeners
                .computeIfAbsent(eventType, ignored -> new ListenerSlots())
                .bucket(order)
                .addIfAbsent(registration)) {
            dispatchPlans.clear();
        }

        return () -> removeRegistration(registry, key, registration);
    }

    private <T extends Event> boolean unsubscribe(
            ListenerRegistry registry,
            @NotNull Class<T> eventType,
            @NotNull EventOrder order,
            @NotNull Consumer<? super T> listener
    ) {
        final Key key = new Key(eventType, order, listener);
        final RegisteredListener registration = registry.registrations.get(key);
        if (registration == null) {
            return false;
        }
        return removeRegistration(registry, key, registration);
    }

    private boolean removeRegistration(
            ListenerRegistry registry,
            Key key,
            RegisteredListener registration
    ) {
        final ListenerSlots slots = registry.listeners.get(key.type());
        if (slots == null || !slots.bucket(key.order()).remove(registration)) {
            return false;
        }

        registry.registrations.remove(key, registration);
        dispatchPlans.clear();
        return true;
    }

    private RegisteredListener[] buildDispatchPlan(DispatchPlanKey key) {
        final List<RegisteredListener> dispatchPlan = new ArrayList<>();

        for (EventOrder order : EVENT_ORDERS) {
            final int orderIndex = order.ordinal();
            for (Class<?> dispatchType : dispatchTypesFor(key.eventType())) {
                appendListeners(dispatchPlan, dispatchType, orderIndex, key.internalOnly());
            }
            appendListeners(dispatchPlan, Event.class, orderIndex, key.internalOnly());
        }

        return dispatchPlan.isEmpty() ? NO_LISTENERS : dispatchPlan.toArray(RegisteredListener[]::new);
    }

    private Class<?>[] dispatchTypesFor(Class<?> eventType) {
        return dispatchTypes.computeIfAbsent(eventType, this::resolveDispatchTypes);
    }

    private Class<?>[] resolveDispatchTypes(Class<?> eventType) {
        LinkedHashSet<Class<?>> resolved = new LinkedHashSet<>();
        collectDispatchTypes(eventType, resolved);
        resolved.remove(Event.class);
        return resolved.toArray(Class<?>[]::new);
    }

    private void collectDispatchTypes(Class<?> type, LinkedHashSet<Class<?>> resolved) {
        if (type == null || !Event.class.isAssignableFrom(type) || !resolved.add(type)) {
            return;
        }

        for (Class<?> iface : type.getInterfaces()) {
            collectDispatchTypes(iface, resolved);
        }

        collectDispatchTypes(type.getSuperclass(), resolved);
    }

    private void appendListeners(
            List<RegisteredListener> dispatchPlan,
            Class<?> eventType,
            int orderIndex,
            boolean internalOnly
    ) {
        List<RegisteredListener> internalBucket = internalListeners.bucket(eventType, orderIndex);
        List<RegisteredListener> publicBucket = internalOnly ? null : publicListeners.bucket(eventType, orderIndex);
        SortedMerge.into(dispatchPlan, internalBucket, publicBucket, RegisteredListener::sequence);
    }

    private record DispatchPlanKey(Class<?> eventType, boolean internalOnly) {
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

    private static final class ListenerSlots {

        @SuppressWarnings("unchecked")
        private final CopyOnWriteArrayList<RegisteredListener>[] buckets = new CopyOnWriteArrayList[EVENT_ORDERS.length];

        private ListenerSlots() {
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new CopyOnWriteArrayList<>();
            }
        }

        private CopyOnWriteArrayList<RegisteredListener> bucket(EventOrder order) {
            return bucket(order.ordinal());
        }

        private CopyOnWriteArrayList<RegisteredListener> bucket(int orderIndex) {
            return buckets[orderIndex];
        }
    }

    private static final class ListenerRegistry {

        private final Map<Class<?>, ListenerSlots> listeners = new ConcurrentHashMap<>();
        private final Map<Key, RegisteredListener> registrations = new ConcurrentHashMap<>();

        private List<RegisteredListener> bucket(Class<?> eventType, int orderIndex) {
            ListenerSlots slots = listeners.get(eventType);
            return slots == null ? null : slots.bucket(orderIndex);
        }
    }

    private static final class RegisteredListener {

        private final Consumer<?> original;
        private final Consumer<? super Event> dispatcher;
        private final long sequence;

        private RegisteredListener(
                Consumer<?> original,
                Consumer<? super Event> dispatcher,
                long sequence
        ) {
            this.original = original;
            this.dispatcher = dispatcher;
            this.sequence = sequence;
        }

        private static <T extends Event> RegisteredListener typed(
                Class<T> eventType,
                Consumer<? super T> listener,
                long sequence
        ) {
            return new RegisteredListener(listener, event -> listener.accept(eventType.cast(event)), sequence);
        }

        private void accept(Event event) {
            dispatcher.accept(event);
        }

        private long sequence() {
            return sequence;
        }

        private String listenerClassName() {
            return original.getClass().getName();
        }
    }
}
