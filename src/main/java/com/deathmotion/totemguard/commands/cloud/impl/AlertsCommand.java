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
import com.deathmotion.totemguard.commands.cloud.arguments.PlayerSuggestion;
import com.deathmotion.totemguard.manager.AlertManagerImpl;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.MessengerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class AlertsCommand extends AbstractCommand {

    private final MessengerService messengerService;
    private final CommandMessengerService commandMessengerService;
    private final AlertManagerImpl alertManager;

    public AlertsCommand(final TotemGuard plugin) {
        this.messengerService = plugin.getMessengerService();
        this.commandMessengerService = messengerService.getCommandMessengerService();
        this.alertManager = plugin.getAlertManager();
    }

    private static boolean isSelf(@NonNull final Player player, @NonNull final Player target) {
        return player.getUniqueId().equals(target.getUniqueId());
    }

    @Override
    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("alerts", Description.of("Toggle alerts for yourself or another player"))
                .optional("target", PlayerParser.playerParser(), PlayerSuggestion.onlinePlayerSuggestions())
                .permission(perm("Alerts"))
                .handler(this::handle)
        );
    }

    private void handle(@NonNull final CommandContext<CommandSender> ctx) {
        final CommandSender sender = ctx.sender();
        final Player target = ctx.getOrDefault("target", null);

        if (sender instanceof ConsoleCommandSender console) {
            handleConsole(console, target);
            return;
        }

        if (sender instanceof Player player) {
            handlePlayer(player, target);
            return;
        }

        sender.sendMessage(Component.text("This command can only be used by players or from the console.", NamedTextColor.RED));
    }

    private void handleConsole(@NonNull final ConsoleCommandSender console, final Player target) {
        if (target == null) {
            console.sendMessage(commandMessengerService.specifyPlayer());
            return;
        }
        toggleForTarget(console, target);
    }

    private void handlePlayer(@NonNull final Player player, final Player target) {
        if (target == null || isSelf(player, target)) {
            alertManager.toggleAlerts(player);
            return;
        }

        if (!player.hasPermission(perm("alerts.others"))) {
            player.sendMessage(messengerService.toggleAlertsOtherNoPermission());
            return;
        }

        toggleForTarget(player, target);
    }

    private void toggleForTarget(@NonNull final CommandSender sender, @NonNull final Player target) {
        final boolean success = alertManager.toggleAlerts(target);
        if (!success) {
            sender.sendMessage(messengerService.toggleAlertsBlockedExternal());
            return;
        }

        final boolean nowEnabled = alertManager.hasAlertsEnabled(target);
        final Component feedback = messengerService.toggleAlertsOther(nowEnabled, target.getName());
        sender.sendMessage(feedback);
    }
}
