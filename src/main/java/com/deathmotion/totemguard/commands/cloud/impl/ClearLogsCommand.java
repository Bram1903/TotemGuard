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

package com.deathmotion.totemguard.commands.cloud.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.cloud.AbstractCommand;
import com.deathmotion.totemguard.commands.cloud.arguments.PlayerSuggestion;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.impl.ClearLogsMessageService;
import com.deathmotion.totemguard.util.MessageUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.Arrays;

public final class ClearLogsCommand extends AbstractCommand {

    private final TotemGuard plugin;
    private final DatabaseProvider databaseProvider;
    private final CommandMessengerService commandMessengerService;
    private final ClearLogsMessageService clearLogsMessageService;

    public ClearLogsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseProvider = plugin.getDatabaseProvider();
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
        this.clearLogsMessageService = plugin.getMessengerService().getClearLogsMessageService();
    }

    @Override
    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("clearlogs", Description.of("Clears the logs of a player"))
                .required("target", StringParser.stringParser(), PlayerSuggestion.onlinePlayerSuggestions())
                .permission(perm("ClearLogs"))
                .handler(this::handle)
        );
    }

    private void handle(@NonNull final CommandContext<CommandSender> ctx) {
        final CommandSender sender = ctx.sender();
        final String username = ctx.get("target");

        sender.sendMessage(MessageUtil.getPrefix().append(Component.text(" Clearing logs for " + username + "...", NamedTextColor.GRAY)));

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            long startTime = System.currentTimeMillis();

            OfflinePlayer offlinePlayer = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);

            if (offlinePlayer == null) {
                sender.sendMessage(MessageUtil.getPrefix().append(Component.text(" Player not found", NamedTextColor.RED)));
                return;
            }

            int deletedRecords = databaseProvider.getGenericService().eraseLogs(offlinePlayer.getUniqueId());

            if (deletedRecords == -1) {
                sender.sendMessage(commandMessengerService.noDatabasePlayerFound(username));
                return;
            }

            long loadTime = System.currentTimeMillis() - startTime;
            sender.sendMessage(clearLogsMessageService.logsCleared(deletedRecords, offlinePlayer.getName(), loadTime));
        });
    }
}
