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
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.gui.screen.history.PlayerHistoryHubScreen;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Opens the History GUI for a player. Unlike /tg profile, the target does not need to be online —
 * we fall back to a database name lookup.
 */
public final class HistoryCommand extends AbstractCommand {

    private static @Nullable UUID tryParseUuid(String input) {
        // UUIDs contain dashes; a valid Minecraft name never does.
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
                        .literal("history")
                        .required(
                                "player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProvider()
                        )
                        .permission(perm("gui.history"))
                        .handler(this::openHistoryGui)
        );
    }

    private void openHistoryGui(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        String input = context.get("player");
        UUID maybeUuid = tryParseUuid(input);

        // Online match is only reliable for names. A raw UUID skips straight to DB lookup
        // so staff can target the old holder of a recycled name.
        if (maybeUuid == null) {
            TGPlayer online = TGPlayerSuggestionProvider.findPlayer(input);
            if (online != null) {
                openFor(sender, online.getUuid(), online.getName());
                return;
            }
        }

        DatabaseRepositoryImpl database = TGPlatform.getInstance().getDatabaseRepository();
        if (!database.isConnected()) {
            sender.sendMessage(Component.text(
                    "'" + input + "' is not online and the database is unavailable.",
                    NamedTextColor.RED
            ));
            return;
        }

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            PlayerRecord record;
            try {
                record = maybeUuid != null
                        ? database.findPlayerByUuid(maybeUuid)
                        : database.findPlayerByName(input);
            } catch (Exception ex) {
                sender.sendMessage(Component.text(
                        "Lookup failed: " + ex.getMessage(),
                        NamedTextColor.RED
                ));
                return;
            }

            if (record == null) {
                sender.sendMessage(Component.text(
                        "No TotemGuard records found for '" + input + "'.",
                        NamedTextColor.RED
                ));
                return;
            }

            openFor(sender, record.uuid(), record.name());
        });
    }

    private void openFor(Sender sender, UUID targetId, String targetName) {
        if (!TGPlatform.getInstance().getGuiManager().open(
                sender, new PlayerHistoryHubScreen(targetId, targetName)
        )) {
            sender.sendMessage(Component.text("Failed to open the history GUI.", NamedTextColor.RED));
        }
    }
}
