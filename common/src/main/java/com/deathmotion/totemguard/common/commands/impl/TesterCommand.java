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
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.features.alert.AlertFilter;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.features.alert.AlertSubscription;
import com.deathmotion.totemguard.common.features.alert.RealtimeAlertRoster;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.UUID;

public final class TesterCommand extends AbstractCommand {

    public static final Duration TESTER_TOGGLE_TTL = Duration.ofMinutes(30);

    private final TGPlatform platform;
    private final AlertRepositoryImpl alertRepository;
    private final CacheRepositoryImpl cacheRepository;

    public TesterCommand() {
        this.platform = TGPlatform.getInstance();
        this.alertRepository = platform.getAlertRepository();
        this.cacheRepository = platform.getCacheRepository();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("tester")
                        .permission(perm("Tester"))
                        .handler(this::toggleTester)
        );
    }

    private void toggleTester(final @NotNull CommandContext<Sender> context) {
        final Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        UUID uuid = sender.getUniqueId();
        RealtimeAlertRoster roster = alertRepository.getRealtimeRoster();
        AlertSubscription current = roster.get(uuid);

        boolean enabled;
        if (current != null && current.filter() instanceof AlertFilter.Self) {
            roster.remove(uuid);
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TESTER_DISABLED));
            enabled = false;
        } else {
            if (sender.hasPermission("TotemGuard.Bypass")) {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TESTER_BYPASS));
                return;
            }
            PlatformPlayer viewer = platform.getPlatformPlayerFactory().create(uuid);
            if (viewer == null) return;
            roster.put(uuid, viewer, new AlertFilter.Self(uuid), null);
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TESTER_ENABLED));
            enabled = true;
        }

        platform.getScheduler().runAsyncTask(() -> cacheRepository.put(
                CacheKeys.testerToggle(uuid), enabled, CacheCodecs.BOOLEAN, TESTER_TOGGLE_TTL));
    }
}
