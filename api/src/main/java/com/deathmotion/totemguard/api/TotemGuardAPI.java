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

package com.deathmotion.totemguard.api;

import com.deathmotion.totemguard.api.alert.AlertRepository;
import com.deathmotion.totemguard.api.config.ConfigRepository;
import com.deathmotion.totemguard.api.event.EventBus;
import com.deathmotion.totemguard.api.history.HistoryRepository;
import com.deathmotion.totemguard.api.host.LoaderInfo;
import com.deathmotion.totemguard.api.mod.ModDetectionRepository;
import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.api.placeholder.PlaceholderRepository;
import com.deathmotion.totemguard.api.punishment.PunishmentRepository;
import com.deathmotion.totemguard.api.redis.RedisRepository;
import com.deathmotion.totemguard.api.stats.StatsRepository;
import com.deathmotion.totemguard.api.update.UpdateCheckerRepository;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Primary entry point for accessing the TotemGuard API.
 */
public interface TotemGuardAPI {

    /**
     * The running TotemGuard plugin version (e.g. {@code 3.1.0}). Advances every release.
     * Use {@link #getApiVersion()} for compatibility checks.
     */
    @NotNull TGVersion getVersion();

    /**
     * The {@code totemguard-api} jar version shaded into this build. Independent of the
     * plugin version, so an API 1.0.x consumer runs against any TotemGuard that ships an
     * API 1.x jar.
     */
    @NotNull TGVersion getApiVersion();

    /**
     * The event bus. Subscribe through channels obtained via {@link EventBus#get(Class)}.
     */
    @NotNull EventBus getEventBus();

    /**
     * Returns the configuration repository, the entry point for reading TotemGuard's YAML
     * files and triggering reloads. Reads return atomic snapshots.
     */
    @NotNull ConfigRepository getConfigRepository();

    /**
     * Returns the user repository for in-memory lookups of {@link com.deathmotion.totemguard.api.user.TGUser}
     * instances by UUID. Only tracks online players, offline UUIDs return {@code null}.
     */
    @NotNull UserRepository getUserRepository();

    /**
     * Returns the placeholder repository for resolving {@code %key%} placeholders and
     * registering custom holders that contribute keys to that resolution.
     */
    @NotNull PlaceholderRepository getPlaceholderRepository();

    /**
     * Returns the punishment repository, which exposes whether a punishment is currently
     * queued or in-flight for a given player. Reflects the cross-server lock when Redis
     * is enabled.
     */
    @NotNull PunishmentRepository getPunishmentRepository();

    /**
     * Returns the Redis repository, a read-only view of TotemGuard's Redis client used
     * to feature-gate cross-server functionality.
     */
    @NotNull RedisRepository getRedisRepository();

    /**
     * Returns the alert repository, a UUID-keyed toggle for staff alert subscriptions.
     * Equivalent to the alert-toggle methods on {@link com.deathmotion.totemguard.api.user.TGUser}
     * but usable without a user handle.
     */
    @NotNull AlertRepository getAlertRepository();

    /**
     * Paginated alert and punishment history. The same data the in-game history GUI shows,
     * exposed via a strict pagination API capped at {@link HistoryRepository#pageSize()} rows
     * per call.
     */
    @NotNull HistoryRepository getHistoryRepository();

    /**
     * Aggregate alert and punishment statistics across every server sharing the database.
     * Results are cached briefly so repeated calls are cheap.
     */
    @NotNull StatsRepository getStatsRepository();

    /**
     * Cross-server network view. Accessors degrade gracefully when Redis is offline.
     */
    @NotNull NetworkRepository getNetworkRepository();

    /**
     * Returns the update checker repository, or empty when running under the loader
     * (the loader owns release discovery in that mode, see {@link #getLoaderInfo()}).
     */
    @NotNull Optional<UpdateCheckerRepository> getUpdateCheckerRepository();

    /**
     * Repository for the mod detection subsystem. Detections accumulate per session and
     * resolve on the next tick into a
     * {@link com.deathmotion.totemguard.api.event.events.TGModDetectionResolvedEvent}.
     */
    @NotNull ModDetectionRepository getModDetectionRepository();

    /**
     * Returns loader info if this plugin is loader-managed, otherwise empty.
     */
    @NotNull Optional<LoaderInfo> getLoaderInfo();
}
