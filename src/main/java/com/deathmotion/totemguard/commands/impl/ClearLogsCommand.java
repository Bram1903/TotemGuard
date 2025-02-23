/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.CommandSuggestionUtil;
import com.deathmotion.totemguard.commands.OfflinePlayerCommandHandler;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.impl.ClearLogsMessageService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

public class ClearLogsCommand {
    private final DatabaseProvider databaseProvider;
    private final CommandMessengerService commandMessengerService;
    private final ClearLogsMessageService clearLogsMessageService;

    public ClearLogsCommand(TotemGuard plugin) {
        this.databaseProvider = plugin.getDatabaseProvider();
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
        this.clearLogsMessageService = plugin.getMessengerService().getClearLogsMessageService();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("clearlogs")
                .withPermission("TotemGuard.ClearLogs")
                .withArguments(new AsyncOfflinePlayerArgument("target").replaceSuggestions(
                        CommandSuggestionUtil.getOfflinePlayerNameSuggestions()
                ))
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        CompletableFuture<OfflinePlayer> target = (CompletableFuture<OfflinePlayer>) args.get("target");
        String targetRawName = args.getRaw("target");
        sender.sendMessage(clearLogsMessageService.clearingStarted());

        OfflinePlayerCommandHandler.handlePlayerTarget(sender, target, targetRawName, this::handleCommand);
    }

    private void handleCommand(CommandSender sender, OfflinePlayer offlinePlayer, String rawUsername) {
        long startTime = System.currentTimeMillis();
        int deletedRecords = databaseProvider.getGenericService().eraseLogs(offlinePlayer.getUniqueId());

        if (deletedRecords == -1) {
            sender.sendMessage(commandMessengerService.noDatabasePlayerFound(rawUsername));
            return;
        }

        long loadTime = System.currentTimeMillis() - startTime;
        sender.sendMessage(clearLogsMessageService.logsCleared(deletedRecords, offlinePlayer.getName(), loadTime));
    }
}
