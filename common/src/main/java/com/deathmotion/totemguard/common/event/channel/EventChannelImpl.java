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

import com.deathmotion.totemguard.api.event.Cancellable;
import com.deathmotion.totemguard.api.event.EventChannel;
import com.deathmotion.totemguard.api.event.EventPriority;
import com.deathmotion.totemguard.api.event.Subscription;
import com.deathmotion.totemguard.common.TGPlatform;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.logging.Level;

public abstract class EventChannelImpl<E> extends ChannelBase<Consumer<? super E>> implements EventChannel<E> {

    private final Class<E> eventType;
    private final boolean cancellable;

    protected EventChannelImpl(@NotNull Class<E> eventType) {
        this.eventType = eventType;
        this.cancellable = Cancellable.class.isAssignableFrom(eventType);
    }

    @Override
    public final @NotNull Class<E> eventType() {
        return eventType;
    }

    @Override
    public final @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler) {
        return subscribe(pluginContext, handler, EventPriority.NORMAL, false);
    }

    @Override
    public final @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler, int priority) {
        return subscribe(pluginContext, handler, priority, false);
    }

    @Override
    public final @NotNull Subscription subscribe(@NotNull Object pluginContext, @NotNull Consumer<? super E> handler,
                                                 int priority, boolean ignoreCancelled) {
        addEntry(handler, priority, ignoreCancelled, pluginContext);
        return () -> unsubscribe(handler);
    }

    protected final void dispatch(@NotNull E event) {
        Entry<Consumer<? super E>>[] arr = entries();
        if (arr.length == 0) return;

        if (cancellable) {
            Cancellable c = (Cancellable) event;
            for (Entry<Consumer<? super E>> e : arr) {
                if (c.isCancelled() && !e.ignoreCancelled) continue;
                try {
                    e.handler.accept(event);
                } catch (Throwable t) {
                    logHandlerException(e, t);
                }
            }
        } else {
            for (Entry<Consumer<? super E>> e : arr) {
                try {
                    e.handler.accept(event);
                } catch (Throwable t) {
                    logHandlerException(e, t);
                }
            }
        }
    }

    private void logHandlerException(Entry<Consumer<? super E>> entry, Throwable t) {
        Object owner = entry.pluginContext;
        String ownerName = owner == null ? "<unknown>" : owner.getClass().getName();
        TGPlatform platform = TGPlatform.getInstance();
        if (platform == null) return;
        platform.getLogger().log(Level.WARNING,
                "Handler for " + eventType.getSimpleName() + " (owner " + ownerName + ") threw",
                t);
    }
}
