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

package com.deathmotion.totemguard.common.cache.impl;

import com.deathmotion.totemguard.common.cache.AbstractCache;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class InternalCache implements AbstractCache {

    private final Cache<UUID, List<CheckSnapshot>> checkCache =
            CacheBuilder.newBuilder()
                    .maximumSize(10_000)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();

    @Override
    public @Blocking void saveCheckSnapshot(UUID uuid, List<CheckSnapshot> checkSnapshots) {
        checkCache.put(uuid, checkSnapshots);
    }

    @Override
    public @Blocking @Nullable List<CheckSnapshot> getCheckSnapshot(UUID uuid) {
        return checkCache.getIfPresent(uuid);
    }
}