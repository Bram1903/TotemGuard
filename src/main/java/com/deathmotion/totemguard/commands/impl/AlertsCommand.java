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

package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.manager.AlertManagerImpl;
import com.deathmotion.totemguard.messenger.impl.CommandMessengerService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AlertsCommand {

    private final CommandMessengerService commandMessengerService;
    private final AlertManagerImpl alertManager;

    public AlertsCommand(TotemGuard plugin) {
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
        this.alertManager = plugin.getAlertManager();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("alerts")
                .withPermission("TotemGuard.Alerts")
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player").replaceSuggestions(ArgumentSuggestions.strings(info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new))))
                .executesConsole(this::handleConsoleCommand)
                .executesPlayer(this::handlePlayerCommand);
    }

    private void handleConsoleCommand(CommandSender sender, CommandArguments args) {
        if (args.getOptional("player").isEmpty()) {
            sender.sendMessage(commandMessengerService.specifyPlayer());
            return;
        }

        Player target = (Player) args.get("player");
        executeToggleAlerts(sender, target);
    }

    private void handlePlayerCommand(Player player, CommandArguments args) {
        if (args.getOptional("player").isEmpty()) {
            alertManager.toggleAlerts(player);
            return;
        }

        if (!player.hasPermission("TotemGuard.Alerts.Others")) {
            player.sendMessage("You do not have permission to toggle alerts for other players!");
            return;
        }

        Player target = (Player) args.get("player");
        executeToggleAlerts(player, target);
    }

    private void executeToggleAlerts(CommandSender sender, Player target) {
        boolean success = alertManager.toggleAlerts(target);

        String message = success ? "Alerts toggled for " + target.getName() : "Failed to toggle alerts for " + target.getName() + ". This could be due to the API event being cancelled.";
        sender.sendMessage(message);
    }
}
