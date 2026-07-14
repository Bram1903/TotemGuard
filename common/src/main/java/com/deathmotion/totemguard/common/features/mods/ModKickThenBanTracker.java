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

package com.deathmotion.totemguard.common.features.mods;

import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public final class ModKickThenBanTracker {

    private final CacheRepositoryImpl cache;

    public ModKickThenBanTracker(CacheRepositoryImpl cache) {
        this.cache = cache;
    }

    public void recordWarning(@NotNull UUID uuid, @NotNull Set<String> warnedModIds, @NotNull Duration window) {
        if (warnedModIds.isEmpty() || window.isZero() || window.isNegative()) return;
        cache.put(CacheKeys.modKickThenBanWarning(uuid), Set.copyOf(warnedModIds), CacheCodecs.STRING_SET, window);
    }

    public @NotNull Set<String> activeWarning(@NotNull UUID uuid) {
        Set<String> stored = cache.get(CacheKeys.modKickThenBanWarning(uuid), CacheCodecs.STRING_SET);
        return stored == null ? Set.of() : stored;
    }

    public void clear(@NotNull UUID uuid) {
        cache.remove(CacheKeys.modKickThenBanWarning(uuid));
    }
}
