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
import com.deathmotion.totemguard.common.network.PresenceListener;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class ModLogAlertTracker implements PresenceListener {

    private static final Duration RETENTION = Duration.ofMinutes(10);

    private final CacheRepositoryImpl cache;

    public ModLogAlertTracker(CacheRepositoryImpl cache) {
        this.cache = cache;
    }

    public @NotNull Set<String> alreadyLogged(@NotNull UUID uuid) {
        Set<String> stored = cache.getAndRefresh(CacheKeys.modLogAlerted(uuid), CacheCodecs.STRING_SET, RETENTION);
        return stored == null ? Set.of() : stored;
    }

    public void markLogged(@NotNull UUID uuid, @NotNull Set<String> modIds) {
        if (modIds.isEmpty()) return;
        Set<String> current = alreadyLogged(uuid);
        Set<String> merged = new LinkedHashSet<>(current.size() + modIds.size());
        merged.addAll(current);
        merged.addAll(modIds);
        cache.put(CacheKeys.modLogAlerted(uuid), Set.copyOf(merged), CacheCodecs.STRING_SET, RETENTION);
    }

    @Override
    public void onPlayerOffline(UUID playerUuid, RemotePlayerEntry lastKnown) {
        cache.remove(CacheKeys.modLogAlerted(playerUuid));
    }
}
