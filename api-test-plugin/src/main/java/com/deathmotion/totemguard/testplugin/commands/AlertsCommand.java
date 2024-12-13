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

package com.deathmotion.totemguard.testplugin.commands;

import com.deathmotion.totemguard.testplugin.ApiTestPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class AlertsCommand extends BukkitCommand {

    private final ApiTestPlugin plugin;

    public AlertsCommand(ApiTestPlugin plugin) {
        super("alerts");
        setDescription("Toggles alerts for totemguard alerts.");
        setUsage("/alerts <player>");
        setAliases(List.of("tgalerts"));
        setPermission("testplugin.alerts");

        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return false;
        }

        if (!plugin.getApi().isApiEnabled()) {
            sender.sendMessage(Component.text("The TotemGuard API is not enabled.", NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            plugin.getApi().alertManager().toggleAlerts(player);
            return true;
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(Component.text("Player not found", NamedTextColor.RED));
                return false;
            }

            plugin.getApi().alertManager().toggleAlerts(target);
            boolean hasAlertsEnabled = plugin.getApi().alertManager().hasAlertsEnabled(target);
            NamedTextColor color = hasAlertsEnabled ? NamedTextColor.GREEN : NamedTextColor.RED;
            sender.sendMessage(Component.text("Alerts for " + target.getName() + " are now " + (hasAlertsEnabled ? "enabled!" : "disabled!"), color));

            return true;
        } else {
            sender.sendMessage(Component.text("Usage: /alerts [player]", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String argsLowerCase = args[0].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
