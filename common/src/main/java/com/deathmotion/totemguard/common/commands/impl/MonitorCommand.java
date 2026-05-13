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

import com.deathmotion.totemguard.api.event.impl.TGMonitorOpenEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.api.impl.TGMonitorOpenEventImpl;
import com.deathmotion.totemguard.common.gui.screen.player.PlayerMonitorScreen;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.network.ServerIdentity;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public final class MonitorCommand extends AbstractCommand {

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("monitor")
                        .required(
                                "tg_player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProvider()
                        )
                        .permission(PlayerMonitorScreen.PERMISSION)
                        .handler(this::openMonitorGui)
        );
    }

    private void openMonitorGui(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();
        String rawTarget = context.get("tg_player");

        UUID targetUuid;
        String targetName;
        PlayerMonitorScreen screen;
        TGPlayer localTarget;
        UUID targetServerInstanceId;
        String targetServerName;

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        ServerIdentity self = presence != null ? presence.identity() : null;

        TGPlayer local = TGPlayerSuggestionProvider.findPlayer(rawTarget);
        if (local != null) {
            targetUuid = local.getUuid();
            targetName = local.getName();
            screen = new PlayerMonitorScreen(local);
            localTarget = local;
            targetServerInstanceId = self != null ? self.instanceId() : new UUID(0L, 0L);
            targetServerName = presence != null ? presence.getLocalServerName() : "";
        } else {
            RemotePlayerEntry remote = TGPlayerSuggestionProvider.findNetworkPlayer(rawTarget);
            if (remote == null) {
                sender.sendMessage(messages.getComponent(
                        MessagesKeys.GENERAL_PLAYER_NOT_FOUND,
                        Map.of("tg_input", rawTarget)
                ));
                return;
            }
            targetUuid = remote.playerUuid();
            targetName = remote.playerName();
            screen = new PlayerMonitorScreen(targetUuid, targetName);
            localTarget = null;
            targetServerInstanceId = remote.serverInstanceId();
            targetServerName = remote.serverName();
        }

        if (sender.getUniqueId().equals(targetUuid)) {
            sender.sendMessage(messages.getComponent(MessagesKeys.MONITOR_SELF));
            return;
        }

        if (local == null) {
            RemotePlayerEntry presenceEntry = presence == null ? null : presence.findByUuid(targetUuid);
            if (presenceEntry != null && presenceEntry.bypassed()) {
                sender.sendMessage(messages.getComponent(
                        MessagesKeys.MONITOR_TARGET_BYPASSED,
                        Map.of("tg_player", targetName, "tg_server", presenceEntry.serverName())
                ));
                return;
            }
        }

        boolean crossServer = self == null || !self.instanceId().equals(targetServerInstanceId);
        TGMonitorOpenEvent event = platform.getEventRepository().post(
                new TGMonitorOpenEventImpl(
                        sender.getUniqueId(), targetUuid, targetName, localTarget,
                        targetServerInstanceId, targetServerName, crossServer, false)
        );
        if (event.isCancelled()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.MONITOR_BLOCKED));
            return;
        }

        if (!platform.getGuiManager().open(sender, screen)) {
            sender.sendMessage(messages.getComponent(MessagesKeys.MONITOR_OPEN_FAILED));
        }
    }
}
