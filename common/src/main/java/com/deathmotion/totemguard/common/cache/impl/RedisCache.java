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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.AbstractCache;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.redis.RedisKeys;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.GetExArgs;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class RedisCache implements AbstractCache {

    private static final int CHECK_SNAPSHOT_TTL = 5 * 60;

    private static final GetExArgs CHECK_SNAPSHOT_GETEX = GetExArgs.Builder.ex(CHECK_SNAPSHOT_TTL);

    private final RedisRepositoryImpl redis = TGPlatform.getInstance().getRedisRepository();

    @Override
    @Blocking
    public void saveCheckSnapshot(UUID uuid, List<CheckSnapshot> checkSnapshots) {
        var async = redis.async();
        if (async == null) return;

        try {
            byte[] payload = CheckSnapshot.encodeList(checkSnapshots);
            async.setex(RedisKeys.checkSnapshots(uuid), CHECK_SNAPSHOT_TTL, payload)
                    .toCompletableFuture()
                    .join();
        } catch (Exception ignored) {
        }
    }

    @Override
    @Blocking
    public @Nullable List<CheckSnapshot> getCheckSnapshot(UUID uuid) {
        var async = redis.async();
        if (async == null) return null;

        try {
            byte[] raw = async.getex(
                    RedisKeys.checkSnapshots(uuid),
                    CHECK_SNAPSHOT_GETEX
            ).toCompletableFuture().join();

            if (raw == null) return null;

            return CheckSnapshot.decodeList(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}