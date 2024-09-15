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
import com.deathmotion.totemguard.commands.totemguard.database.ClearCommand;
import com.deathmotion.totemguard.commands.totemguard.database.TrimCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseCommand implements SubCommand {
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public DatabaseCommand(TotemGuard plugin) {
        subCommands.put("trim", new TrimCommand(plugin));
        subCommands.put("clear", new ClearCommand(plugin));
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(getAvailableCommandsComponent(sender));
            return false;
        }

        String subCommandName = args[1].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand != null && hasPermissionForSubCommand(sender, subCommandName)) {
            return subCommand.execute(sender, args);
        } else {
            sender.sendMessage(getAvailableCommandsComponent(sender));
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .filter(name -> hasPermissionForSubCommand(sender, name))
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[1].toLowerCase());
            if (subCommand != null && hasPermissionForSubCommand(sender, args[1].toLowerCase())) {
                return subCommand.onTabComplete(sender, args);
            }
        }
        return List.of();
    }

    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        return switch (subCommand) {
            case "trim" -> sender.hasPermission("TotemGuard.Database.Trim");
            case "clear" -> sender.hasPermission("TotemGuard.Database.Clear");
            default -> false;
        };
    }

    private Component getAvailableCommandsComponent(CommandSender sender) {
        // Start building the help message
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("TotemGuard Database Commands", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Below are the available subcommands:", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline());

        // Command descriptions
        Map<String, String> commandDescriptions = Map.of(
                "trim", "Clears all logs that are older than 30 days.",
                "clear", "Clears all logs from the database."
        );

        // Add each command to the message if the sender has permission
        for (String command : subCommands.keySet()) {
            if (hasPermissionForSubCommand(sender, command)) {
                componentBuilder.append(Component.text("- ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("/totemguard database " + command, NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(commandDescriptions.get(command), NamedTextColor.GRAY))
                        .append(Component.newline());
            }
        }

        return componentBuilder.build();
    }
}
