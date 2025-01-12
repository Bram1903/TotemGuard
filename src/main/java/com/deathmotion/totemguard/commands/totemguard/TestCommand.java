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
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class TestCommand {

    private final TotemGuard plugin;

    public TestCommand(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("test")
                .withPermission("TotemGuard.Test")
                .withArguments(new AsyncOfflinePlayerArgument("target").replaceSuggestions(
                        ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() ->
                                Stream.of(Bukkit.getOfflinePlayers())
                                        .map(OfflinePlayer::getName)
                                        .toArray(String[]::new)
                        ))
                ))
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        CompletableFuture<OfflinePlayer> target = (CompletableFuture<OfflinePlayer>) args.get("target");
        if (target == null) return;

        sender.sendMessage(Component.text("Searching for player...", NamedTextColor.GRAY));
        target.thenAccept(offlinePlayer -> {
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(Component.text("Player never joined before", NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("Offline player " + offlinePlayer.getName() + " found", NamedTextColor.GREEN));
        }).exceptionally(throwable -> {
            Throwable cause = throwable.getCause();
            Throwable rootCause = cause instanceof RuntimeException ? cause.getCause() : cause;

            sender.sendMessage(Component.text(rootCause.getMessage(), NamedTextColor.RED));
            return null;
        });
    }
}
