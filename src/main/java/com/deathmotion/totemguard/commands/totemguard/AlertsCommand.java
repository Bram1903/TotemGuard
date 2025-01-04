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
import com.deathmotion.totemguard.manager.AlertManagerImpl;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.SafeSuggestions;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AlertsCommand {

    private final AlertManagerImpl alertManager;

    public AlertsCommand(TotemGuard plugin) {
        this.alertManager = plugin.getAlertManager();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("alerts")
                .withOptionalArguments(new PlayerArgument("player").replaceSafeSuggestions(SafeSuggestions.suggest(info -> Bukkit.getOnlinePlayers().toArray(new Player[0]))))
                .executesConsole(this::handleConsoleCommand)
                .executesPlayer(this::handlePlayerCommand);
    }

    private void handleConsoleCommand(CommandSender sender, CommandArguments args) {
        if (args.getOptional("player").isEmpty()) {
            sender.sendMessage("You need to specify a player when running this command from the console.");
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

        Player target = (Player) args.get("player");
        executeToggleAlerts(player, target);
    }

    private void executeToggleAlerts(CommandSender sender, Player target) {
        boolean success = alertManager.toggleAlerts(target);

        String message = success ? "Alerts toggled for " + target.getName() : "Failed to toggle alerts for " + target.getName() + ". This could be due to the API event being cancelled.";
        sender.sendMessage(message);
    }
}
