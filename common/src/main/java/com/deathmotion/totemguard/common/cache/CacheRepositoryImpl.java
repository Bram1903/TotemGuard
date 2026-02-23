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

package com.deathmotion.totemguard.common.cache;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.cache.impl.InternalCache;
import com.deathmotion.totemguard.common.cache.impl.RedisCache;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class CacheRepositoryImpl {

    private final RedisRepositoryImpl redisRepository;

    private final InternalCache internalCache;
    private final RedisCache redisCache;

    public CacheRepositoryImpl() {
        this.redisRepository = TGPlatform.getInstance().getRedisRepository();

        this.internalCache = new InternalCache();
        this.redisCache = new RedisCache();
    }

    public void saveCheckSnapshot(UUID uuid, List<CheckSnapshot> checkSnapshots) {
        getCache().saveCheckSnapshot(uuid, checkSnapshots);
    }

    public @Nullable List<CheckSnapshot> getCheckSnapshot(UUID uuid) {
        return getCache().getCheckSnapshot(uuid);
    }

    private AbstractCache getCache() {
        return redisRepository.isConnected() ? redisCache : internalCache;
    }
}
