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

package com.deathmotion.totemguard.commands.totemguard.database;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.SubCommand;
import com.deathmotion.totemguard.database.DatabaseService;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Random;

public class TrimCommand implements SubCommand {
    private final TotemGuard plugin;
    private final DatabaseService databaseService;

    private final Component invalidCodeComponent;
    private final Component trimStartedComponent;

    private String randomCode = null;

    public TrimCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseService = plugin.getDatabaseService();

        invalidCodeComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text("Invalid code. Please use the code provided.", NamedTextColor.RED))
                .build();

        trimStartedComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text("Database trimming started...", NamedTextColor.GREEN))
                .build();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            randomCode = generateRandomCode();
            sender.sendMessage(confirmationComponent(randomCode));
            return false;
        }

        if (randomCode == null || !randomCode.equals(args[2])) {
            sender.sendMessage(invalidCodeComponent);
            return false;
        }

        sender.sendMessage(trimStartedComponent);
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o -> {
            long startTime = System.currentTimeMillis();
            int totalRemovedLogs = databaseService.trimDatabase();
            long loadTime = System.currentTimeMillis() - startTime;

            Component message = Component.text()
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                    .append(Component.text("Trimmed " + totalRemovedLogs + " logs in " + loadTime + "ms.", NamedTextColor.GREEN))
                    .build();

            sender.sendMessage(message);
        }));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private Component confirmationComponent(String randomCode) {
        String command = "/totemguard database trim " + randomCode;

        return Component.text()
                .append(Component.text("[WARNING]: ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("You are about to trim the database.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("This action will remove all logs older than 30 days.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Type ", NamedTextColor.GRAY))
                .append(Component.text(command, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(" or click ", NamedTextColor.GRAY))
                .append(Component.text("[Trim Database]", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to run: " + command, NamedTextColor.GRAY))))
                .append(Component.text(" to confirm.", NamedTextColor.GRAY))
                .build();
    }

}

