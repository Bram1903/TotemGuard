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

package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.common.check.CheckImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChatBuffer {
    private final CheckImpl check;
    private final AtomicInteger violations = new AtomicInteger();
    private volatile @Nullable String debug;
    private volatile @NotNull Map<String, Object> extras = Map.of();

    ChatBuffer(CheckImpl check) {
        this.check = check;
    }

    void update(int newViolations, @Nullable String debug, @NotNull Map<String, Object> extras) {
        violations.accumulateAndGet(newViolations, Math::max);

        if (debug != null) {
            this.debug = debug;
        }
        if (!extras.isEmpty()) {
            this.extras = Map.copyOf(extras);
        }
    }

    CheckImpl getCheck() {
        return check;
    }

    int getViolations() {
        return violations.get();
    }

    @Nullable String getDebug() {
        return debug;
    }

    @NotNull Map<String, Object> getExtras() {
        return extras;
    }
}
