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
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.AlertsToggleData;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.cache.data.VPNData;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.options.RedisKeys;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RedisCache implements AbstractCache {

    private final RedisRepositoryImpl redis;
    private final CacheRepositoryImpl cacheRepository;
    private final Logger logger;

    public RedisCache(CacheRepositoryImpl cacheRepository, RedisRepositoryImpl redisRepository) {
        this.redis = redisRepository;
        this.cacheRepository = cacheRepository;
        this.logger = TGPlatform.getInstance().getLogger();
    }

    @Override
    public void saveVPNData(@NonNull String ip, @NonNull VPNData vpnData) {
        try {
            set(
                    RedisKeys.vpnData(ip),
                    cacheRepository.getVpnDataTTL(),
                    vpnData.encode(),
                    "Failed to save VPN data to Redis for ip " + ip
            );
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to encode VPN data for ip " + ip, exception);
        }
    }

    @Override
    public @Nullable VPNData getVPNData(@NonNull String ip) {
        byte[] raw = get(
                RedisKeys.vpnData(ip),
                cacheRepository.getVpnDataTTL(),
                "Failed to get VPN data from Redis for ip " + ip
        );

        if (raw == null) return null;

        try {
            return new VPNData(false).decode(raw);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to decode VPN data for ip " + ip, exception);
            return null;
        }
    }

    @Override
    @Blocking
    public void saveCheckSnapshot(@NonNull UUID uuid, @NonNull List<CheckSnapshot> checkSnapshots) {
        try {
            set(
                    RedisKeys.checkSnapshots(uuid),
                    cacheRepository.getCheckDataTTL(),
                    CheckSnapshot.encodeList(checkSnapshots),
                    "Failed to save check snapshots to Redis for uuid " + uuid
            );
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to encode check snapshots for uuid " + uuid, exception);
        }
    }

    @Override
    @Blocking
    public @Nullable List<CheckSnapshot> getCheckSnapshot(@NonNull UUID uuid) {
        byte[] raw = get(
                RedisKeys.checkSnapshots(uuid),
                cacheRepository.getCheckDataTTL(),
                "Failed to get check snapshots from Redis for uuid " + uuid
        );

        if (raw == null) return null;

        try {
            return CheckSnapshot.decodeList(raw);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to decode check snapshots for uuid " + uuid, exception);
            return null;
        }
    }

    @Override
    public void saveAlertsToggleData(@NotNull UUID uuid, @NotNull AlertsToggleData alertsToggleData) {
        try {
            set(
                    RedisKeys.alertsToggleData(uuid),
                    cacheRepository.getAlertsToggleDataTTL(),
                    alertsToggleData.encode(),
                    "Failed to save alerts toggle data to Redis for uuid " + uuid
            );
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to encode alerts toggle data for uuid " + uuid, exception);
        }
    }

    @Override
    public @Nullable AlertsToggleData getAlertsToggleData(@NotNull UUID uuid) {
        byte[] raw = get(
                RedisKeys.alertsToggleData(uuid),
                cacheRepository.getAlertsToggleDataTTL(),
                "Failed to get alerts toggle data from Redis for uuid " + uuid
        );

        if (raw == null) return null;

        try {
            return new AlertsToggleData(false).decode(raw);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to decode alerts toggle data for uuid " + uuid, exception);
            return null;
        }
    }

    private void set(byte[] key, int ttl, byte[] payload, String logMessage) {
        RedisCommands<byte[], byte[]> sync = redis.sync();
        if (sync == null) return;

        try {
            sync.setex(key, ttl, payload);
        } catch (Exception exception) {
            logger.log(Level.WARNING, logMessage, exception);
        }
    }

    private byte @Nullable [] get(byte[] key, int ttl, String logMessage) {
        RedisCommands<byte[], byte[]> sync = redis.sync();
        if (sync == null) return null;

        try {
            return sync.getex(key, GetExArgs.Builder.ex(ttl));
        } catch (Exception exception) {
            logger.log(Level.WARNING, logMessage, exception);
            return null;
        }
    }
}