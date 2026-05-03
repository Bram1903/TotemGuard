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
import com.deathmotion.totemguard.api.event.EventRepository;
import com.deathmotion.totemguard.api.history.HistoryRepository;
import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.api.placeholder.PlaceholderRepository;
import com.deathmotion.totemguard.api.punishment.PunishmentRepository;
import com.deathmotion.totemguard.api.redis.RedisRepository;
import com.deathmotion.totemguard.api.stats.StatsRepository;
import com.deathmotion.totemguard.api.update.UpdateCheckerRepository;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Primary entry point for accessing the TotemGuard API.
 */
public interface TotemGuardAPI {

    /**
     * Returns the version of the running TotemGuard plugin (e.g. {@code 3.1.0}).
     * <p>
     * This advances every plugin release; consumers should not depend on its exact value
     * for compatibility checks. Use {@link #getApiVersion()} for that.
     *
     * @return the plugin version, never {@code null}
     */
    @NotNull TGVersion getVersion();

    /**
     * Returns the version of the {@code totemguard-api} jar shaded into this build of
     * TotemGuard. Independent of the plugin version: an API 1.0.x consumer shading
     * {@code totemguard-api:1.0.0} can run against TotemGuard 3.1, 3.2, 3.3, etc. as long
     * as the plugin still ships an API 1.x jar.
     *
     * @return the API version, never {@code null}
     */
    @NotNull TGVersion getApiVersion();

    /**
     * Returns the repository for event subscription and dispatch.
     *
     * @return the event repository, never {@code null}
     */
    @NotNull EventRepository getEventRepository();

    /**
     * Returns the repository for configuration management.
     *
     * @return the configuration repository, never {@code null}
     */
    @NotNull ConfigRepository getConfigRepository();

    /**
     * Returns the repository for user management.
     *
     * @return the user repository, never {@code null}
     */
    @NotNull UserRepository getUserRepository();

    /**
     * Returns the repository for placeholder management.
     *
     * @return the placeholder repository, never {@code null}
     */
    @NotNull PlaceholderRepository getPlaceholderRepository();

    /**
     * Returns the repository for punishment management.
     *
     * @return the punishment repository, never {@code null}
     */
    @NotNull PunishmentRepository getPunishmentRepository();

    /**
     * Returns the Redis repository.
     *
     * @return the redis repository, never {@code null}
     */
    @NotNull RedisRepository getRedisRepository();

    /**
     * Returns the repository for alert management.
     *
     * @return the alert repository, never {@code null}
     */
    @NotNull AlertRepository getAlertRepository();

    /**
     * Returns the repository for paginated alert and punishment history. The same backing
     * data the in-game history GUI shows, exposed through a strict pagination API so a
     * single call can never request more than {@link HistoryRepository#pageSize()} rows.
     *
     * @return the history repository, never {@code null}
     */
    @NotNull HistoryRepository getHistoryRepository();

    /**
     * Returns the repository for aggregate alert and punishment statistics. Counts cover
     * every player on every server sharing the database; results are cached for a short
     * window so repeated calls are cheap.
     *
     * @return the statistics repository, never {@code null}
     */
    @NotNull StatsRepository getStatsRepository();

    /**
     * Returns the cross-server network view: connected backend count, tracked
     * player count, this server's display name, and Redis connection state.
     * <p>
     * Useful for dashboards and integrations that want to know how big the
     * TotemGuard fleet currently is. All accessors degrade gracefully when
     * Redis is offline.
     *
     * @return the network repository, never {@code null}
     */
    @NotNull NetworkRepository getNetworkRepository();

    /**
     * Returns the repository that tracks the latest published TotemGuard release.
     * <p>
     * Reads of {@link UpdateCheckerRepository#latestKnownVersion()} are cheap
     * and reflect data shared across the fleet via Redis. Use
     * {@link UpdateCheckerRepository#checkNow()} to trigger a fresh HTTP fetch
     * when needed; the result is propagated to other servers automatically.
     *
     * @return the update checker repository, never {@code null}
     */
    @NotNull UpdateCheckerRepository getUpdateCheckerRepository();
}
