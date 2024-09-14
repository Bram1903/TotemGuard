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
import com.deathmotion.totemguard.commands.totemguard.AlertsCommand;
import com.deathmotion.totemguard.commands.totemguard.InfoCommand;
import com.deathmotion.totemguard.commands.totemguard.LogsCommand;
import com.deathmotion.totemguard.commands.totemguard.ReloadCommand;
import com.deathmotion.totemguard.data.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TotemGuardCommand implements CommandExecutor, TabExecutor {
    private final Component versionComponent;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public TotemGuardCommand(TotemGuard plugin) {
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("alerts", new AlertsCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin.getConfigManager()));
        subCommands.put("logs", new LogsCommand(plugin));

        versionComponent = Component.text()
                .append(Component.text("⚡", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" Running ", NamedTextColor.GRAY))
                .append(Component.text("TotemGuard", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text(" v" + plugin.getVersion().toString(), NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text(" by ", NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .append(Component.text("Bram", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text(" and ", NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .append(Component.text("OutDev", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .hoverEvent(HoverEvent.showText(Component.text("Open Github Page!", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .decorate(TextDecoration.UNDERLINED)))
                .clickEvent(ClickEvent.openUrl(Constants.GITHUB_URL))
                .build();

        plugin.getCommand("totemguard").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAnyPermission(sender)) {
            sender.sendMessage(versionComponent);
            return true;
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
        return subCommands.keySet().stream().anyMatch(command -> hasPermissionForSubCommand(sender, command));
    }

    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        return switch (subCommand) {
            case "info" -> true;
            case "alerts" -> sender.hasPermission("TotemGuard.Alerts");
            case "reload" -> sender.hasPermission("TotemGuard.Reload");
            case "logs" -> sender.hasPermission("TotemGuard.Logs");
            default -> false;
        };
    }

    private Component getAvailableCommandsComponent(CommandSender sender) {
        String availableCommands = subCommands.keySet().stream()
                .filter(name -> hasPermissionForSubCommand(sender, name))
                .collect(Collectors.joining("|"));

        return Component.text("Usage: /totemguard <" + availableCommands + ">", NamedTextColor.RED);
    }
}