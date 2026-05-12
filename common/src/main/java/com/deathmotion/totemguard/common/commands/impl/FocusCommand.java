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

import com.deathmotion.totemguard.api.event.impl.TGFocusEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.FocusTarget;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.api.impl.TGFocusEventImpl;
import com.deathmotion.totemguard.common.features.alert.AlertFilter;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.features.alert.AlertSubscription;
import com.deathmotion.totemguard.common.features.alert.RealtimeAlertRoster;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public final class FocusCommand extends AbstractCommand {

    public static final Duration FOCUS_TTL = Duration.ofMinutes(10);

    private final TGPlatform platform;
    private final AlertRepositoryImpl alertRepository;
    private final CacheRepositoryImpl cacheRepository;

    public FocusCommand() {
        this.platform = TGPlatform.getInstance();
        this.alertRepository = platform.getAlertRepository();
        this.cacheRepository = platform.getCacheRepository();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("focus")
                        .optional(
                                "tg_player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProviderExcludingSelf()
                        )
                        .permission(perm("focus"))
                        .handler(this::toggleFocus)
        );
    }

    private void toggleFocus(final @NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        UUID viewerUuid = sender.getUniqueId();
        RealtimeAlertRoster roster = alertRepository.getRealtimeRoster();
        String rawTarget = context.<String>optional("tg_player").orElse(null);

        if (rawTarget == null) {
            AlertSubscription current = roster.get(viewerUuid);
            if (current != null && current.filter() instanceof AlertFilter.Violator) {
                TGFocusEvent disable = platform.getEventRepository().post(TGFocusEventImpl.disabling(viewerUuid));
                if (disable.isCancelled()) return;
                roster.remove(viewerUuid);
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOCUS_DISABLED));
                platform.getScheduler().runAsyncTask(() -> cacheRepository.remove(CacheKeys.focusTarget(viewerUuid)));
            } else {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOCUS_NONE_ACTIVE));
            }
            return;
        }

        UUID targetUuid;
        String targetName;
        TGPlayer localTarget;
        UUID targetServerInstanceId;
        String targetServerName;

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        TGPlayer local = TGPlayerSuggestionProvider.findPlayer(rawTarget);
        if (local != null) {
            targetUuid = local.getUuid();
            targetName = local.getName();
            localTarget = local;
            targetServerInstanceId = presence.identity().instanceId();
            targetServerName = presence.getLocalServerName();
        } else {
            RemotePlayerEntry remote = TGPlayerSuggestionProvider.findNetworkPlayer(rawTarget);
            if (remote == null) {
                sender.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.FOCUS_NOT_FOUND,
                        Map.of("tg_input", rawTarget)
                ));
                return;
            }
            targetUuid = remote.playerUuid();
            targetName = remote.playerName();
            localTarget = null;
            targetServerInstanceId = remote.serverInstanceId();
            targetServerName = remote.serverName();
        }

        if (targetUuid.equals(viewerUuid)) {
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOCUS_SELF));
            return;
        }

        AlertSubscription current = roster.get(viewerUuid);
        if (current != null
                && current.filter() instanceof AlertFilter.Violator violator
                && violator.target().equals(targetUuid)) {
            TGFocusEvent disable = platform.getEventRepository().post(TGFocusEventImpl.disabling(viewerUuid));
            if (disable.isCancelled()) return;
            roster.remove(viewerUuid);
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOCUS_DISABLED));
            platform.getScheduler().runAsyncTask(() -> cacheRepository.remove(CacheKeys.focusTarget(viewerUuid)));
            return;
        }

        PlatformPlayer viewer = platform.getPlatformPlayerFactory().create(viewerUuid);
        if (viewer == null) return;

        TGFocusEvent enable = platform.getEventRepository().post(TGFocusEventImpl.enabling(
                viewerUuid, targetUuid, targetName, localTarget,
                targetServerInstanceId, targetServerName, false));
        if (enable.isCancelled()) return;

        roster.put(viewerUuid, viewer, new AlertFilter.Violator(targetUuid), targetName);
        sender.sendMessage(platform.getMessageService().getComponent(
                MessagesKeys.FOCUS_ENABLED,
                Map.of("tg_player", targetName)
        ));
        platform.getScheduler().runAsyncTask(() -> cacheRepository.put(
                CacheKeys.focusTarget(viewerUuid),
                new FocusTarget(targetUuid, targetName),
                CacheCodecs.FOCUS_TARGET, FOCUS_TTL));
    }
}
