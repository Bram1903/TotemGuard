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
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class BukkitTgLoaderCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("restart", "status");

    private final JavaPlugin plugin;
    private final PluginRuntime runtime;

    public BukkitTgLoaderCommand(JavaPlugin plugin, PluginRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    private static boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (!isConsole(sender)) return false;

        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "restart" -> handleRestart(sender);
            case "status" -> {
                String inner = runtime.loadedVersion();
                sender.sendMessage("Loader: " + LoaderManifest.loaderVersion());
                sender.sendMessage("Inner: " + (inner == null ? "NOT LOADED" : inner));
            }
            default -> sender.sendMessage("Usage: /tgloader <restart|status>");
        }
        return true;
    }

    private void handleRestart(CommandSender sender) {
        sender.sendMessage("Restarting TotemGuard. Resolving from configured source...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                runtime.restart();
                String version = runtime.loadedVersion();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        "TotemGuard restarted. Running version " + (version == null ? "unknown" : version) + "."));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard restart failed", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        "Restart failed: " + t.getMessage() + ". Loader stays online. Check console for details."));
            }
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!isConsole(sender)) return Collections.emptyList();
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
