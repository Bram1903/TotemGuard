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
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.AlertsToggleData;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class AlertCommand extends AbstractCommand {

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
            cacheManager.saveCheckToggleData(sender.getUniqueId(), new AlertsToggleData(enabled));
        });
    }
}
