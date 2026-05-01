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
import com.deathmotion.totemguard.common.gui.screen.PlayerMonitorScreen;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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

        TGPlayer target = resolveTarget(sender, context.get("tg_player"));
        if (target == null) {
            return;
        }

        if (sender.getUniqueId().equals(target.getUuid())) {
            sender.sendMessage(messages.getComponent(MessagesKeys.MONITOR_SELF));
            return;
        }

        TGMonitorOpenEvent event = platform.getEventRepository().post(
                new TGMonitorOpenEventImpl(sender.getUniqueId(), target.getUuid())
        );
        if (event.isCancelled()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.MONITOR_BLOCKED));
            return;
        }

        if (!platform.getGuiManager().open(sender, new PlayerMonitorScreen(target))) {
            sender.sendMessage(messages.getComponent(MessagesKeys.MONITOR_OPEN_FAILED));
        }
    }

    private TGPlayer resolveTarget(Sender sender, String rawTarget) {
        TGPlayer target = TGPlayerSuggestionProvider.findPlayer(rawTarget);
        if (target != null) {
            return target;
        }

        sender.sendMessage(TGPlatform.getInstance().getMessageService().getComponent(
                MessagesKeys.GENERAL_PLAYER_NOT_FOUND,
                Map.of("tg_input", rawTarget)
        ));
        return null;
    }
}
