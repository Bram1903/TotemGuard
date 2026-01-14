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

package com.deathmotion.totemguard.common.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-safe lazy initialization with memoization.
 */
public final class Lazy<T> {

    private volatile T value;
    private Supplier<? extends T> supplier;

    private Lazy(Supplier<? extends T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
        return new Lazy<>(supplier);
    }

    public T get() {
        T local = value;
        if (local != null) return local;

        synchronized (this) {
            local = value;
            if (local == null) {
                local = Objects.requireNonNull(supplier.get(), "supplier returned null");
                value = local;

                // Release supplier reference after init to avoid retaining captured objects.
                supplier = null;
            }
            return local;
        }
    }

    public boolean isInitialized() {
        return value != null;
    }
}
