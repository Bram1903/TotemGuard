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

package com.deathmotion.totemguard.proxybridge.common.state;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class BackendTracker {

    private final Object lock = new Object();
    private final Set<String> lastPublished = new HashSet<>();

    public @NotNull Diff diff(@NotNull Set<String> current) {
        synchronized (lock) {
            Set<String> added = new HashSet<>(current);
            added.removeAll(lastPublished);
            Set<String> removed = new HashSet<>(lastPublished);
            removed.removeAll(current);
            return new Diff(added, removed);
        }
    }

    public void recordPublished(@NotNull Set<String> current) {
        synchronized (lock) {
            lastPublished.clear();
            lastPublished.addAll(current);
        }
    }

    public int size() {
        synchronized (lock) {
            return lastPublished.size();
        }
    }

    public record Diff(@NotNull Set<String> added, @NotNull Set<String> removed) {
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }
    }
}
