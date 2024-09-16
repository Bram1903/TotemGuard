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
import com.deathmotion.totemguard.mojang.ApiResponse;
import com.deathmotion.totemguard.mojang.MojangService;
import com.deathmotion.totemguard.mojang.models.BadRequest;
import com.deathmotion.totemguard.mojang.models.Found;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClearLogsCommand implements SubCommand {
    private final TotemGuard plugin;
    private final MojangService mojangService;
    private final DatabaseService databaseService;

    private final Component usageComponent;
    private final Component clearingStartedComponent;

    public ClearLogsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.mojangService = plugin.getMojangService();
        this.databaseService = plugin.getDatabaseService();

        usageComponent = Component.text("Usage: /totemguard clearlogs <player>", NamedTextColor.RED);
        clearingStartedComponent = Component.text("Clearing logs...", NamedTextColor.GREEN);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(usageComponent);
            return false;
        }

        sender.sendMessage(clearingStartedComponent);
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            ApiResponse response = mojangService.getUUID(args[1]);
            if (!handleApiResponse(sender, response)) {
                return;
            }

            Found found = (Found) response;
            UUID uuid = found.uuid();


            long startTime = System.currentTimeMillis();
            int deletedAlerts = databaseService.clearAlerts(uuid);
            int deletedPunishments = databaseService.clearPunishments(uuid);
            int deletedLogs = deletedAlerts + deletedPunishments;
            long loadTime = System.currentTimeMillis() - startTime;

            sender.sendMessage(Component.text("Cleared " + deletedLogs + " logs for" + found.username() + " in " + loadTime + "ms", NamedTextColor.GREEN));
        });
        return true;
    }

    private boolean handleApiResponse(CommandSender sender, ApiResponse response) {
        if (response == null) {
            sender.sendMessage(Component.text("An error occurred while fetching the player's UUID.", NamedTextColor.RED));
            return false;
        }

        return switch (response.responseStatus()) {
            case 204 -> {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                yield false;
            }
            case 400 -> {
                sender.sendMessage(Component.text(((BadRequest) response).errorMessage(), NamedTextColor.RED));
                yield false;
            }
            case 429 -> {
                sender.sendMessage(Component.text("Rate limit exceeded. Please try again later.", NamedTextColor.RED));
                yield false;
            }
            default -> true;
        };
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
}
