/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.commands.totemguard;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.SubCommand;
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.mojang.MojangService;
import com.deathmotion.totemguard.mojang.models.Callback;
import com.deathmotion.totemguard.util.MessageService;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ClearLogsCommand implements SubCommand {
    private final TotemGuard plugin;
    private final MojangService mojangService;
    private final DatabaseService databaseService;
    private final MessageService messageService;

    public ClearLogsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.mojangService = plugin.getMojangService();
        this.databaseService = plugin.getDatabaseService();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(getUsageComponent());
            return false;
        }

        sender.sendMessage(getClearingStartedComponent());
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            Callback response = mojangService.getUUID(args[1]);
            if (!handleApiResponse(sender, response)) {
                return;
            }

            long startTime = System.currentTimeMillis();
            int deletedRecords = databaseService.clearLogs(response.getUuid());
            long loadTime = System.currentTimeMillis() - startTime;

            sender.sendMessage(getClearedLogsComponent(deletedRecords, response.getUsername(), loadTime));
        });
        return true;
    }

    private boolean handleApiResponse(CommandSender sender, Callback response) {
        if (response.getUsername() == null) {
            sender.sendMessage(response.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String argsLowerCase = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private Component getUsageComponent() {
        return messageService.getPrefix()
                .append(Component.text("Usage: /totemguard clearlogs <player>", NamedTextColor.RED));
    }

    private Component getClearingStartedComponent() {
        return messageService.getPrefix()
                .append(Component.text("Clearing logs...", NamedTextColor.GREEN));
    }

    private Component getClearedLogsComponent(int deletedRecords, String username, long loadTime) {
        return messageService.getPrefix()
                .append(Component.text("Cleared " + deletedRecords + " logs for " + username + " in " + loadTime + "ms", NamedTextColor.GREEN));
    }
}
