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
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.AlertsToggleData;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.cache.data.VPNData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class InternalCache implements AbstractCache {

    private static final long MAX_CACHE_SIZE = 10_000L;

    private final CacheRepositoryImpl cacheRepository;

    private Cache<UUID, List<CheckSnapshot>> checkCache;
    private Cache<String, VPNData> vpnCache;
    private Cache<UUID, AlertsToggleData> alertsToggleCache;

    public InternalCache(CacheRepositoryImpl cacheRepository) {
        this.cacheRepository = cacheRepository;
        this.checkCache = buildCheckCache();
        this.alertsToggleCache = buildAlertsToggleCache();
        this.vpnCache = buildVpnCache();
    }

    public void reload() {
        Map<UUID, List<CheckSnapshot>> checkEntries = checkCache.asMap();
        Map<UUID, AlertsToggleData> alertsToggleCache = this.alertsToggleCache.asMap();
        Map<String, VPNData> vpnEntries = vpnCache.asMap();

        Cache<UUID, List<CheckSnapshot>> newCheckCache = buildCheckCache();
        Cache<UUID, AlertsToggleData> newAlertsToggleCache = buildAlertsToggleCache();
        Cache<String, VPNData> newVpnCache = buildVpnCache();

        if (cacheRepository.isCacheEnabled() && cacheRepository.getCheckDataTTL() > 0) {
            newCheckCache.putAll(checkEntries);
        }

        if (cacheRepository.isCacheEnabled() && cacheRepository.getAlertsToggleDataTTL() > 0) {
            newAlertsToggleCache.putAll(alertsToggleCache);
        }

        if (cacheRepository.isCacheEnabled() && cacheRepository.getVpnDataTTL() > 0) {
            newVpnCache.putAll(vpnEntries);
        }

        this.checkCache.invalidateAll();
        this.alertsToggleCache.invalidateAll();
        this.vpnCache.invalidateAll();

        this.checkCache = newCheckCache;
        this.alertsToggleCache = newAlertsToggleCache;
        this.vpnCache = newVpnCache;
    }

    @Override
    public void saveVPNData(@NonNull String ip, @NonNull VPNData vpnData) {
        if (!cacheRepository.isCacheEnabled() || cacheRepository.getVpnDataTTL() <= 0) return;
        vpnCache.put(ip, vpnData);
    }

    @Override
    public @Nullable VPNData getVPNData(@NonNull String ip) {
        if (!cacheRepository.isCacheEnabled() || cacheRepository.getVpnDataTTL() <= 0) return null;
        return vpnCache.getIfPresent(ip);
    }

    @Override
    public @Blocking void saveCheckSnapshot(@NonNull UUID uuid, @NonNull List<CheckSnapshot> checkSnapshots) {
        if (!cacheRepository.isCacheEnabled() || cacheRepository.getCheckDataTTL() <= 0) return;
        checkCache.put(uuid, checkSnapshots);
    }

    @Override
    public @Blocking @Nullable List<CheckSnapshot> getCheckSnapshot(@NonNull UUID uuid) {
        if (!cacheRepository.isCacheEnabled() || cacheRepository.getCheckDataTTL() <= 0) return null;
        return checkCache.getIfPresent(uuid);
    }

    @Override
    public void saveAlertsToggleData(@NotNull UUID uuid, @NotNull AlertsToggleData alertsToggleData) {
        if (!cacheRepository.isCacheEnabled() || cacheRepository.getAlertsToggleDataTTL() <= 0) return;
        alertsToggleCache.put(uuid, alertsToggleData);
    }

    @Override
    public @Nullable AlertsToggleData getAlertsToggleData(@NotNull UUID uuid) {
        if (!cacheRepository.isCacheEnabled() || cacheRepository.getAlertsToggleDataTTL() <= 0) return null;
        return alertsToggleCache.getIfPresent(uuid);
    }

    private Cache<UUID, List<CheckSnapshot>> buildCheckCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE);

        if (cacheRepository.isCacheEnabled() && cacheRepository.getCheckDataTTL() > 0) {
            builder.expireAfterAccess(cacheRepository.getCheckDataTTL(), TimeUnit.SECONDS);
        }

        return builder.build();
    }

    private Cache<UUID, AlertsToggleData> buildAlertsToggleCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE);

        if (cacheRepository.isCacheEnabled() && cacheRepository.getAlertsToggleDataTTL() > 0) {
            builder.expireAfterAccess(cacheRepository.getAlertsToggleDataTTL(), TimeUnit.SECONDS);
        }

        return builder.build();
    }

    private Cache<String, VPNData> buildVpnCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE);

        if (cacheRepository.isCacheEnabled() && cacheRepository.getVpnDataTTL() > 0) {
            builder.expireAfterAccess(cacheRepository.getVpnDataTTL(), TimeUnit.SECONDS);
        }

        return builder.build();
    }
}