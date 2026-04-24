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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.UUID;

public final class AlertCommand extends AbstractCommand {

    private static final Duration ALERTS_TOGGLE_TTL = Duration.ofMinutes(30);

    private final TGPlatform platform;
    private final AlertRepositoryImpl alertRepository;
    private final CacheRepositoryImpl cacheManager;

    public AlertCommand() {
        this.platform = TGPlatform.getInstance();
        this.alertRepository = platform.getAlertRepository();
        this.cacheManager = platform.getCacheRepository();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("alerts")
                        .permission(perm("alerts"))
                        .handler(this::toggleAlerts)
        );
    }

    private void toggleAlerts(final @NotNull CommandContext<Sender> context) {
        final Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        boolean enabled = alertRepository.toggleAlerts(sender.getUniqueId());

        platform.getScheduler().runAsyncTask(() -> {
            UUID uuid = sender.getUniqueId();

            // Cache is the fast path — read on every login, updated here so
            // the next reconnect within the TTL window doesn't need the DB.
            cacheManager.put(CacheKeys.alertsToggle(uuid),
                    enabled, CacheCodecs.BOOLEAN, ALERTS_TOGGLE_TTL);

            // DB is the durable path — it outlives the cache TTL and the
            // process, so staff preferences survive restarts and rejoins
            // days later.
            if (platform.getDatabaseRepository().isConnected()) {
                try {
                    platform.getDatabaseRepository().upsertStaffAlertPref(uuid, enabled);
                } catch (Exception ex) {
                    platform.getLogger().warning(
                            "Failed to persist alert toggle for " + uuid + ": " + ex.getMessage());
                }
            }
        });
    }
}
