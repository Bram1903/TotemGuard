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
import com.deathmotion.totemguard.manager.AlertManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class AlertsCommand implements SubCommand {
    private final TotemGuard plugin;
    private final AlertManager alertManager;

    public AlertsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return toggleAlertsForSender(sender);
        } else if (sender.hasPermission("TotemGuard.Alerts.Others")) {
            return toggleAlertsForOther(sender, args[1]);
        } else {
            Component message = Component.text()
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                    .append(Component.text(" You do not have permission to toggle alerts for other players!", NamedTextColor.RED))
                    .build();

            sender.sendMessage(message);
            return false;
        }
    }

    private boolean toggleAlertsForSender(CommandSender sender) {
        if (sender instanceof Player player) {
            alertManager.toggleAlerts(player);
            return true;
        } else {
            Component message = Component.text()
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                    .append(Component.text(" Only players can toggle alerts!", NamedTextColor.RED))
                    .build();

            sender.sendMessage(message);
            return false;
        }
    }

    private boolean toggleAlertsForOther(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            Component message = Component.text()
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                    .append(Component.text(" Player not found!", NamedTextColor.RED))
                    .build();

            sender.sendMessage(message);
            return false;
        }

        alertManager.toggleAlerts(target);
        boolean alertsEnabled = alertManager.hasAlertsEnabled(target);

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text((alertsEnabled ? " Enabled" : " Disabled") + " alerts for " + target.getName() + "!", alertsEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build();

        sender.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2 && sender.hasPermission("TotemGuard.Alerts.Others")) {
            String argsLowerCase = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
