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

package com.deathmotion.totemguard.common;

import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.alert.AlertRepository;
import com.deathmotion.totemguard.api.cluster.ClusterService;
import com.deathmotion.totemguard.api.config.ConfigRepository;
import com.deathmotion.totemguard.api.event.EventBus;
import com.deathmotion.totemguard.api.history.HistoryRepository;
import com.deathmotion.totemguard.api.host.LoaderInfo;
import com.deathmotion.totemguard.api.loader.LoaderControl;
import com.deathmotion.totemguard.api.mod.ModDetectionRepository;
import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.api.placeholder.PlaceholderRepository;
import com.deathmotion.totemguard.api.punishment.PunishmentRepository;
import com.deathmotion.totemguard.api.redis.RedisRepository;
import com.deathmotion.totemguard.api.stats.StatsRepository;
import com.deathmotion.totemguard.api.update.UpdateCheckerRepository;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.api.versioning.TGAPIVersions;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.common.loader.LoaderControlAdapter;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.host.LoaderController;
import com.deathmotion.totemguard.host.TGPluginHost;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class TGPlatformAPI implements TotemGuardAPI {

    private final TGPlatform platform;

    public TGPlatformAPI() {
        this.platform = TGPlatform.getInstance();
    }

    @Override
    public @NotNull TGVersion getVersion() {
        return TGVersions.CURRENT;
    }

    @Override
    public @NotNull TGVersion getApiVersion() {
        return TGAPIVersions.CURRENT;
    }

    @Override
    public @NotNull EventBus getEventBus() {
        return platform.getEventBus();
    }

    @Override
    public @NotNull ConfigRepository getConfigRepository() {
        return platform.getConfigRepository();
    }

    @Override
    public void reload() {
        platform.getReloadService().reload();
    }

    @Override
    public @NotNull UserRepository getUserRepository() {
        return platform.getPlayerRepository();
    }

    @Override
    public @NotNull PlaceholderRepository getPlaceholderRepository() {
        return platform.getPlaceholderRepository();
    }

    @Override
    public @NotNull PunishmentRepository getPunishmentRepository() {
        return platform.getPunishmentRepository();
    }

    @Override
    public @NotNull RedisRepository getRedisRepository() {
        return platform.getRedisRepository();
    }

    @Override
    public @NotNull AlertRepository getAlertRepository() {
        return platform.getAlertRepository();
    }

    @Override
    public @NotNull HistoryRepository getHistoryRepository() {
        return platform.getHistoryRepository();
    }

    @Override
    public @NotNull StatsRepository getStatsRepository() {
        return platform.getStatsRepository();
    }

    @Override
    public @NotNull Optional<UpdateCheckerRepository> getUpdateCheckerRepository() {
        return Optional.ofNullable(platform.getUpdateCheckerRepository());
    }

    @Override
    public @NotNull NetworkRepository getNetworkRepository() {
        return platform.getNetworkPresenceRepository();
    }

    @Override
    public @NotNull ClusterService getCluster() {
        return platform.getClusterService();
    }

    @Override
    public @NotNull ModDetectionRepository getModDetectionRepository() {
        return platform.getModDetectionService();
    }

    @Override
    public @NotNull Optional<LoaderInfo> getLoaderInfo() {
        TGPluginHost host = platform.getPluginHost();
        if (host == null || !host.managedByLoader()) return Optional.empty();
        return host.loaderController().map(LoaderController::info);
    }

    @Override
    public @NotNull Optional<LoaderControl> getLoaderControl() {
        TGPluginHost host = platform.getPluginHost();
        if (host == null || !host.managedByLoader()) return Optional.empty();
        return host.loaderController().map(LoaderControlAdapter::new);
    }
}
