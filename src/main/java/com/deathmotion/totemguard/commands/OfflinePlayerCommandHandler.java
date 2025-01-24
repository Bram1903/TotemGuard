/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.commands;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class OfflinePlayerCommandHandler {

    public static void handlePlayerTarget(CommandSender sender, CompletableFuture<OfflinePlayer> targetFuture, PlayerCommandAction action) {
        final CommandMessengerService commandMessengerService = TotemGuard.getInstance().getMessengerService().getCommandMessengerService();
        targetFuture.thenAccept(offlinePlayer -> {
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(commandMessengerService.targetNeverJoined());
                return;
            }

            action.execute(sender, offlinePlayer);
        }).exceptionally(throwable -> {
            Throwable cause = throwable.getCause();
            Throwable rootCause = cause instanceof RuntimeException ? cause.getCause() : cause;

            sender.sendMessage(Component.text(rootCause.getMessage(), NamedTextColor.RED));
            return null;
        });
    }

    @FunctionalInterface
    public interface PlayerCommandAction {
        void execute(CommandSender sender, OfflinePlayer offlinePlayer);
    }
}

