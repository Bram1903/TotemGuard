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

package com.deathmotion.totemguard.commands;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.totemguard.*;
import com.deathmotion.totemguard.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TotemGuardCommand implements CommandExecutor, TabExecutor {
    // Immutable map for command descriptions
    private static final Map<String, String> COMMAND_DESCRIPTIONS;

    static {
        Map<String, String> tempMap = new LinkedHashMap<>();
        tempMap.put("info", "Show plugin information.");
        tempMap.put("alerts", "Toggle alerts on/off.");
        tempMap.put("check", "Attempts to bait a player");
        tempMap.put("reload", "Reload the plugin configuration.");
        tempMap.put("profile", "View player profiles.");
        tempMap.put("stats", "View plugin statistics.");
        tempMap.put("clearlogs", "Clear player logs.");
        tempMap.put("track", "Tracks a player.");
        tempMap.put("untrack", "Stops tracking a player.");
        tempMap.put("top", "View the top players based on violations.");
        tempMap.put("database", "Database management commands.");
        COMMAND_DESCRIPTIONS = Collections.unmodifiableMap(tempMap);
    }

    private final MessageService messageService;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public TotemGuardCommand(TotemGuard plugin) {
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("alerts", new AlertsCommand(plugin));
        subCommands.put("check", CheckCommand.getInstance(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("profile", new ProfileCommand(plugin));
        subCommands.put("stats", new StatsCommand(plugin));
        subCommands.put("clearlogs", new ClearLogsCommand(plugin));
        subCommands.put("track", new TrackCommand(plugin));
        subCommands.put("untrack", new UntrackCommand(plugin));
        subCommands.put("top", new TopCommand(plugin));
        subCommands.put("database", new DatabaseCommand(plugin));

        messageService = plugin.getMessageService();
        plugin.getCommand("totemguard").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAnyPermission(sender)) {
            sender.sendMessage(messageService.version());
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(getAvailableCommandsComponent(sender));
            return false;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand != null && hasPermissionForSubCommand(sender, subCommandName)) {
            return subCommand.execute(sender, args);
        } else {
            sender.sendMessage(getAvailableCommandsComponent(sender));
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .filter(name -> hasPermissionForSubCommand(sender, name))
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && hasPermissionForSubCommand(sender, args[0].toLowerCase())) {
                return subCommand.onTabComplete(sender, args);
            }
        }
        return List.of();
    }

    private boolean hasAnyPermission(CommandSender sender) {
        return subCommands.keySet().stream().anyMatch(command -> !command.equals("info") && hasPermissionForSubCommand(sender, command));
    }

    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        return switch (subCommand) {
            case "info" -> true;
            case "alerts" -> sender.hasPermission("TotemGuard.Alerts");
            case "check" -> sender.hasPermission("TotemGuard.Check");
            case "reload" -> sender.hasPermission("TotemGuard.Reload");
            case "profile" -> sender.hasPermission("TotemGuard.Profile");
            case "stats" -> sender.hasPermission("TotemGuard.Stats");
            case "clearlogs" -> sender.hasPermission("TotemGuard.ClearLogs");
            case "track", "untrack" -> sender.hasPermission("TotemGuard.Track");
            case "top" -> sender.hasPermission("TotemGuard.Top");
            case "database" -> hasAnyDatabasePermissions(sender);
            default -> false;
        };
    }

    private Component getAvailableCommandsComponent(CommandSender sender) {
        // Start building the help message
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("TotemGuard Commands", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Below are the available subcommands:", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline());

        // Add each command to the message if the sender has permission
        for (String command : COMMAND_DESCRIPTIONS.keySet()) {
            if (hasPermissionForSubCommand(sender, command)) {
                componentBuilder.append(Component.text("- ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("/totemguard " + command, NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(COMMAND_DESCRIPTIONS.get(command), NamedTextColor.GRAY))
                        .append(Component.newline());
            }
        }

        return componentBuilder.build();
    }

    private boolean hasAnyDatabasePermissions(CommandSender sender) {
        if (sender.hasPermission("TotemGuard.Database")) {
            return true;
        } else if (sender.hasPermission("TotemGuard.Database.Trim")) {
            return true;
        } else return sender.hasPermission("TotemGuard.Database.Clear");
    }
}