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

import com.deathmotion.totemguard.api3.config.key.MessagesKeys;
import com.deathmotion.totemguard.api3.history.HistoryClearResult;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class ClearHistoryCommand extends AbstractCommand {

    private static @Nullable UUID tryParseUuid(String input) {
        if (input.indexOf('-') < 0) return null;
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("clearhistory")
                        .required(
                                "player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProvider()
                        )
                        .permission(perm("ClearHistory"))
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String input = context.get("player");
        UUID maybeUuid = tryParseUuid(input);
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();

        if (maybeUuid == null) {
            TGPlayer online = TGPlayerSuggestionProvider.findPlayer(input);
            if (online != null) {
                runClear(sender, online.getUuid(), online.getName());
                return;
            }
        }

        DatabaseRepositoryImpl database = platform.getDatabaseRepository();
        if (!database.isConnected()) {
            sender.sendMessage(messages.getComponent(
                    MessagesKeys.GENERAL_DATABASE_UNAVAILABLE,
                    Map.of("tg_input", input)
            ));
            return;
        }

        platform.getScheduler().runAsyncTask(() -> {
            PlayerRecord record;
            try {
                record = maybeUuid != null
                        ? database.findPlayerByUuid(maybeUuid)
                        : database.findPlayerByName(input);
            } catch (Exception ex) {
                sender.sendMessage(messages.getComponent(
                        MessagesKeys.GENERAL_LOOKUP_FAILED,
                        Map.of("tg_error", String.valueOf(ex.getMessage()))
                ));
                return;
            }

            if (record == null) {
                sender.sendMessage(messages.getComponent(
                        MessagesKeys.GENERAL_NO_RECORDS,
                        Map.of("tg_input", input)
                ));
                return;
            }

            runClear(sender, record.uuid(), record.name());
        });
    }

    private void runClear(Sender sender, UUID uuid, String name) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();

        sender.sendMessage(messages.getComponent(
                MessagesKeys.CLEARHISTORY_CLEARING,
                Map.of("tg_player", name)
        ));

        platform.getHistoryRepository().clear(uuid).thenAccept(response -> {
            if (response.ok()) {
                HistoryClearResult result = response.value();
                sender.sendMessage(messages.getComponent(
                        MessagesKeys.CLEARHISTORY_CLEARED,
                        Map.of(
                                "tg_player", name,
                                "tg_alerts_removed", result.alertsRemoved(),
                                "tg_punishments_removed", result.punishmentsRemoved()
                        )
                ));
            } else {
                sender.sendMessage(messages.getComponent(
                        MessagesKeys.CLEARHISTORY_CLEAR_FAILED,
                        Map.of("tg_error", String.valueOf(response.message()))
                ));
            }
        });
    }
}
