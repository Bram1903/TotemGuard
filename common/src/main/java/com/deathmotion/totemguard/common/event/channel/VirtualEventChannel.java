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

package com.deathmotion.totemguard.common.event.channel;

import com.deathmotion.totemguard.api.event.EventChannel;
import com.deathmotion.totemguard.api.event.EventPriority;
import com.deathmotion.totemguard.api.event.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public final class VirtualEventChannel<E> implements EventChannel<E> {

    private final Class<E> eventType;
    private final List<EventChannelImpl<? extends E>> children;

    public VirtualEventChannel(@NotNull Class<E> eventType, @NotNull List<EventChannelImpl<? extends E>> children) {
        this.eventType = eventType;
        this.children = children;
    }

    // Child generics are erased at the call boundary. The cast is safe because
    // every child's event type extends E (enforced at registration in EventBusImpl).
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Subscription subscribeChild(EventChannelImpl child, Object pluginContext,
                                               Consumer handler, int priority, boolean ignoreCancelled) {
        return child.subscribe(pluginContext, handler, priority, ignoreCancelled);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void unsubscribeChild(EventChannelImpl child, Consumer handler) {
        child.unsubscribe(handler);
    }

    @Override
    public @NotNull Class<E> eventType() {
        return eventType;
    }

    @Override
    public @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler) {
        return subscribe(pluginContext, handler, EventPriority.NORMAL, false);
    }

    @Override
    public @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler, int priority) {
        return subscribe(pluginContext, handler, priority, false);
    }

    @Override
    public @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler,
                                           int priority, boolean ignoreCancelled) {
        Subscription[] subs = new Subscription[children.size()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = subscribeChild(children.get(i), pluginContext, handler, priority, ignoreCancelled);
        }
        return () -> {
            for (Subscription s : subs) s.unsubscribe();
        };
    }

    @Override
    public void unsubscribe(@NotNull Consumer<? super E> handler) {
        for (EventChannelImpl<? extends E> child : children) {
            unsubscribeChild(child, handler);
        }
    }

    @Override
    public boolean isEmpty() {
        for (EventChannelImpl<? extends E> child : children) {
            if (!child.isEmpty()) return false;
        }
        return true;
    }
}
