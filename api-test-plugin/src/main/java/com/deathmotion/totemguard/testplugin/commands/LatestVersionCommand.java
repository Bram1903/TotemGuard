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
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LatestVersionCommand extends BukkitCommand {

    private final ApiTestPlugin plugin;

    public LatestVersionCommand(ApiTestPlugin plugin) {
        super("latestversion");
        setDescription("Gets the latest version of TotemGuard from the API if available.");
        setUsage("/latestversion");
        setAliases(List.of("lv"));
        setPermission("testplugin.latestversion");

        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!plugin.getApi().isEnabled()) {
            sender.sendMessage(Component.text("The TotemGuard API is not enabled.", NamedTextColor.RED));
            return false;
        }

        plugin.getApi().getLatestVersion().ifPresentOrElse(
                version -> sender.sendMessage(Component.text("The latest version of TotemGuard is " + version + "!", NamedTextColor.GREEN)),
                () -> sender.sendMessage(Component.text("The latest version of TotemGuard is not available.", NamedTextColor.RED))
        );

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
