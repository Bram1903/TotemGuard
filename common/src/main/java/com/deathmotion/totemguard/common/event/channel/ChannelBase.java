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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Shared base used by every channel implementation. Keeps subscribers in a
 * volatile array ordered from low priority to high priority and replaces the
 * whole array whenever someone registers or unregisters. Dispatch code only
 * needs to read the volatile reference once and iterate.
 *
 * @param <H> handler type held by entries on this channel
 */
public abstract class ChannelBase<H> {

    private final Object writeLock = new Object();
    private volatile Entry<H>[] entries;

    @SuppressWarnings("unchecked")
    protected ChannelBase() {
        this.entries = (Entry<H>[]) new Entry[0];
    }

    /**
     * Returns the current entry array. Callers must not modify it. Once the
     * array is handed out, the registry replaces it on the next mutation
     * instead of editing in place, so iterating it is safe even while another
     * thread subscribes.
     */
    protected final @NotNull Entry<H>[] entries() {
        return entries;
    }

    /**
     * {@code true} when no subscribers are registered.
     */
    public final boolean isEmpty() {
        return entries.length == 0;
    }

    /**
     * Inserts a new subscriber. Smaller priority numbers run before larger
     * ones. Subscribers registered at the same priority keep insertion order
     * (FIFO).
     */
    @SuppressWarnings("unchecked")
    protected final void addEntry(@NotNull H handler, int priority, boolean ignoreCancelled,
                                  @Nullable Object pluginContext) {
        synchronized (writeLock) {
            Entry<H>[] old = entries;
            Entry<H>[] next = Arrays.copyOf(old, old.length + 1);
            int i = old.length;
            while (i > 0 && old[i - 1].priority > priority) {
                next[i] = old[i - 1];
                i--;
            }
            next[i] = new Entry<>(handler, priority, ignoreCancelled, pluginContext);
            entries = next;
        }
    }

    /**
     * Removes the first entry whose handler matches by reference identity.
     */
    public final void unsubscribe(@NotNull H handler) {
        removeMatching(e -> e.handler == handler);
    }

    /**
     * Removes every entry whose plugin context equals the argument.
     */
    public final void unsubscribeAllFromPlugin(@NotNull Object pluginContext) {
        removeMatching(e -> pluginContext.equals(e.pluginContext));
    }

    @SuppressWarnings("unchecked")
    private void removeMatching(@NotNull Predicate<Entry<H>> filter) {
        synchronized (writeLock) {
            Entry<H>[] old = entries;
            int keep = 0;
            for (Entry<H> e : old) if (!filter.test(e)) keep++;
            if (keep == old.length) return;
            Entry<H>[] next = (Entry<H>[]) new Entry[keep];
            int idx = 0;
            for (Entry<H> e : old) if (!filter.test(e)) next[idx++] = e;
            entries = next;
        }
    }

    /**
     * Backing record for a single subscriber. Every field is assigned once in
     * the constructor and never touched again, which lets the dispatch loop
     * read them without locking.
     */
    public static final class Entry<H> {

        public final @NotNull H handler;
        public final int priority;
        public final boolean ignoreCancelled;
        public final @Nullable Object pluginContext;

        Entry(@NotNull H handler, int priority, boolean ignoreCancelled, @Nullable Object pluginContext) {
            this.handler = handler;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.pluginContext = pluginContext;
        }
    }
}
