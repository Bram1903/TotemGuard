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

package com.deathmotion.totemguard.loader.fleet;

import com.deathmotion.totemguard.api.fleet.FleetCache;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FleetCacheRef {

    private final Logger logger;
    private final CopyOnWriteArrayList<Consumer<FleetCache>> attachListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<FleetCache>> detachListeners = new CopyOnWriteArrayList<>();

    private volatile @Nullable FleetCache current;
    @Getter
    private volatile boolean apiReady;

    public FleetCacheRef(Logger logger) {
        this.logger = logger;
    }

    public @Nullable FleetCache current() {
        return current;
    }

    public Optional<FleetCache> available() {
        FleetCache c = current;
        return (c != null && c.isHealthy()) ? Optional.of(c) : Optional.empty();
    }

    public synchronized void set(@Nullable FleetCache next) {
        FleetCache previous = this.current;
        if (previous == next) return;
        this.current = next;

        if (previous != null) {
            fire(detachListeners, previous, "detach");
        }
        if (next != null) {
            fire(attachListeners, next, "attach");
        }
    }

    public synchronized void onAttach(Consumer<FleetCache> listener) {
        attachListeners.add(listener);
        FleetCache c = this.current;
        if (c != null) safe(() -> listener.accept(c), "attach listener replay");
    }

    public void onDetach(Consumer<FleetCache> listener) {
        detachListeners.add(listener);
    }

    public void markApiReady() {
        this.apiReady = true;
    }

    public Optional<byte[]> l2Get(String key) {
        FleetCache c = current;
        if (c == null || !c.isHealthy()) return Optional.empty();
        try {
            return c.get(key);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public void l2Put(String key, byte[] value, Duration ttl) {
        FleetCache c = current;
        if (c == null || !c.isHealthy()) return;
        try {
            c.put(key, value, ttl);
        } catch (Throwable ignored) {
        }
    }

    private void fire(Iterable<Consumer<FleetCache>> listeners, FleetCache cache, String label) {
        for (Consumer<FleetCache> listener : listeners) {
            safe(() -> listener.accept(cache), label + " listener");
        }
    }

    private void safe(Runnable action, String label) {
        try {
            action.run();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "FleetCacheRef " + label + " threw", t);
        }
    }
}
