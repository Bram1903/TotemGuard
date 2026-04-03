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

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.data.AlertsToggleData;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.cache.data.PunishQueueData;
import com.deathmotion.totemguard.common.cache.data.VPNData;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.cache.RedisKeys;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheRepositoryImpl {

    private static final long DEFAULT_LOCAL_MAX_ENTRIES = 10_000L;

    private final ConfigRepositoryImpl configRepository;
    private final RedisRepositoryImpl redisRepository;
    private final CacheStore<UUID, List<CheckSnapshot>> checkSnapshotStore;
    private final CacheStore<UUID, PunishQueueData> punishQueueStore;
    private final CacheStore<UUID, AlertsToggleData> alertsToggleStore;
    private final CacheStore<String, VPNData> vpnStore;
    private final Set<UUID> inFlightPunishments = ConcurrentHashMap.newKeySet();
    private volatile boolean cacheEnabled;
    private volatile int checkDataTTL;
    private volatile int punishQueueTTL;
    private volatile int alertsToggleDataTTL;
    private volatile int vpnDataTTL;
    private volatile long localCacheMaxEntries;

    public CacheRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();

        this.configRepository = platform.getConfigRepository();
        this.redisRepository = platform.getRedisRepository();

        loadConfig();

        this.checkSnapshotStore = new CacheStore<>(
                "check snapshots",
                () -> checkDataTTL,
                () -> cacheEnabled,
                () -> localCacheMaxEntries,
                RedisKeys::checkSnapshots,
                CheckSnapshot::encodeList,
                CheckSnapshot::decodeList
        );

        this.punishQueueStore = new CacheStore<>(
                "punish queue data",
                () -> punishQueueTTL,
                () -> cacheEnabled,
                () -> localCacheMaxEntries,
                RedisKeys::punishQueue,
                PunishQueueData::encode,
                bytes -> new PunishQueueData(0L).decode(bytes)
        );

        this.alertsToggleStore = new CacheStore<>(
                "alerts toggle data",
                () -> alertsToggleDataTTL,
                () -> cacheEnabled,
                () -> localCacheMaxEntries,
                RedisKeys::alertsToggleData,
                AlertsToggleData::encode,
                bytes -> new AlertsToggleData(false).decode(bytes)
        );

        this.vpnStore = new CacheStore<>(
                "vpn data",
                () -> vpnDataTTL,
                () -> cacheEnabled,
                () -> localCacheMaxEntries,
                RedisKeys::vpnData,
                VPNData::encode,
                bytes -> new VPNData(false).decode(bytes)
        );
    }

    public void reload() {
        loadConfig();

        checkSnapshotStore.reloadLocalCache();
        punishQueueStore.reloadLocalCache();
        alertsToggleStore.reloadLocalCache();
        vpnStore.reloadLocalCache();
    }

    public void saveVPNData(String ip, VPNData vpnData) {
        vpnStore.put(ip, vpnData);
    }

    public @Nullable VPNData getVPNData(String ip) {
        return vpnStore.get(ip);
    }

    public boolean tryClaimPunishment(UUID uuid) {
        if (!inFlightPunishments.add(uuid)) {
            return false;
        }

        if (!shouldUseDistributedPunishmentLock()) {
            return true;
        }

        boolean claimed = punishQueueStore.putIfAbsent(uuid, PunishQueueData.now());
        if (!claimed) {
            inFlightPunishments.remove(uuid);
        }

        return claimed;
    }

    public boolean isPunishmentQueued(UUID uuid) {
        if (inFlightPunishments.contains(uuid)) {
            return true;
        }

        return shouldUseDistributedPunishmentLock() && punishQueueStore.get(uuid) != null;
    }

    public void finishPunishment(UUID uuid, boolean keepDistributedLock) {
        inFlightPunishments.remove(uuid);

        if (keepDistributedLock && shouldUseDistributedPunishmentLock()) {
            return;
        }

        punishQueueStore.remove(uuid);
    }

    public void saveCheckToggleData(UUID uuid, AlertsToggleData alertsToggleData) {
        alertsToggleStore.put(uuid, alertsToggleData);
    }

    public @Nullable AlertsToggleData getAlertsToggleData(UUID uuid) {
        return alertsToggleStore.get(uuid);
    }

    public void saveCheckSnapshot(UUID uuid, List<CheckSnapshot> checkSnapshots) {
        checkSnapshotStore.put(uuid, checkSnapshots);
    }

    public @Nullable List<CheckSnapshot> getCheckSnapshot(UUID uuid) {
        return checkSnapshotStore.get(uuid);
    }

    private void loadConfig() {
        Config config = configRepository.config(ConfigFile.CONFIG);

        this.cacheEnabled = config.getBoolean(ConfigKeys.CACHE_ENABLED);
        this.checkDataTTL = config.getInt(ConfigKeys.CACHE_DATA_CHECKS);
        this.punishQueueTTL = config.getInt(ConfigKeys.CACHE_PUNISH_QUEUE);
        this.alertsToggleDataTTL = config.getInt(ConfigKeys.CACHE_ALERTS_TOGGLE);
        this.vpnDataTTL = config.getInt(ConfigKeys.CACHE_DATA_VPN);

        long configuredMaxEntries = config.getInt(ConfigKeys.CACHE_LOCAL_MAX_ENTRIES);
        this.localCacheMaxEntries = configuredMaxEntries > 0 ? configuredMaxEntries : DEFAULT_LOCAL_MAX_ENTRIES;
    }

    private boolean shouldUseDistributedPunishmentLock() {
        return cacheEnabled && punishQueueTTL > 0 && redisRepository.isConnected();
    }
}
