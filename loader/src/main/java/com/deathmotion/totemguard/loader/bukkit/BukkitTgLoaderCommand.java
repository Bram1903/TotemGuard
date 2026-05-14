/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.loader.bukkit;

import com.deathmotion.totemguard.loader.core.LoaderManifest;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class BukkitTgLoaderCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("restart", "status", "info");

    private final JavaPlugin plugin;
    private final PluginRuntime runtime;

    public BukkitTgLoaderCommand(JavaPlugin plugin, PluginRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("totemguard.loader.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "restart" -> handleRestart(sender);
            case "info" -> {
                sender.sendMessage("§7TotemGuard Loader §f" + LoaderManifest.loaderVersion());
                String inner = runtime.loadedVersion();
                sender.sendMessage("§7Running inner: §f" + (inner == null ? "none" : inner));
            }
            case "status" -> {
                String inner = runtime.loadedVersion();
                sender.sendMessage("§7Loader: §f" + LoaderManifest.loaderVersion());
                sender.sendMessage("§7Inner: §f" + (inner == null ? "§cNOT LOADED" : inner));
            }
            default -> sender.sendMessage("§eUsage: /tgloader <restart|status|info>");
        }
        return true;
    }

    private void handleRestart(CommandSender sender) {
        sender.sendMessage("§7Restarting TotemGuard. Resolving from configured source...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                runtime.restart();
                String version = runtime.loadedVersion();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        "§aTotemGuard restarted. Running version §f" + (version == null ? "unknown" : version) + "§a."));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard restart failed", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        "§cRestart failed: " + t.getMessage() + ". Loader stays online. Check console for details."));
            }
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
