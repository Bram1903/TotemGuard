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
import com.deathmotion.totemguard.commands.cloud.arguments.PlayerArgument;
import com.deathmotion.totemguard.manager.AlertManagerImpl;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.MessengerService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class AlertsCommand extends AbstractCommand {

    private final TotemGuard plugin;
    private final MessengerService messengerService;
    private final CommandMessengerService commandMessengerService;
    private final AlertManagerImpl alertManager;

    public AlertsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.messengerService = plugin.getMessengerService();
        this.commandMessengerService = messengerService.getCommandMessengerService();
        this.alertManager = plugin.getAlertManager();
    }

    @Override
    public void register(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("alerts", Description.of("Toggle alerts for yourself or another player"))
                .optional(
                        "target",
                        PlayerArgument.playerParser(),
                        PlayerArgument.onlinePlayerSuggestions()
                )
                .permission("TotemGuard.Alerts")
                .handler(this::genericHandler)
        );
    }

    private void genericHandler(@NonNull CommandContext<CommandSender> ctx) {
        final CommandSender sender = ctx.sender();
        Player target = ctx.getOrDefault("target", null);

        if (sender instanceof ConsoleCommandSender console) {
            handleConsole(console, target);
            return;
        }
        if (sender instanceof Player player) {
            handlePlayer(player, target);
            return;
        }

        sender.sendMessage("§cThis command can only be used by players or from the console.");
    }

    private void handleConsole(@NonNull ConsoleCommandSender console, Player target) {
        if (target == null) {
            console.sendMessage(commandMessengerService.specifyPlayer());
            return;
        }
        executeToggleAlerts(console, target);
    }

    private void handlePlayer(@NonNull Player player, Player target) {
        // No target => toggle self
        if (target == null || target.getUniqueId().equals(player.getUniqueId())) {
            alertManager.toggleAlerts(player);
            return;
        }

        // Targeting someone else requires extra permission
        if (!player.hasPermission("TotemGuard.Alerts.Others")) {
            player.sendMessage(messengerService.toggleAlertsOtherNoPermission());
            return;
        }

        executeToggleAlerts(player, target);
    }

    /**
     * Shared execution with Folia + messages, mirrors your old implementation.
     */
    private void executeToggleAlerts(CommandSender sender, Player target) {
        boolean success = alertManager.toggleAlerts(target);
        if (!success) {
            sender.sendMessage(messengerService.toggleAlertsBlockedExternal());
            return;
        }

        boolean enabled = alertManager.hasAlertsEnabled(target);
        Component msg = enabled
                ? messengerService.toggleAlertsOther(true, target.getName())
                : messengerService.toggleAlertsOther(false, target.getName());
        sender.sendMessage(msg);
    }
}
