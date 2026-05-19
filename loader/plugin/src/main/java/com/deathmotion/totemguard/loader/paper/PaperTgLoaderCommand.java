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

package com.deathmotion.totemguard.loader.paper;

import com.deathmotion.totemguard.loader.command.LoaderCommandService;
import com.deathmotion.totemguard.loader.command.LoaderMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PaperTgLoaderCommand implements TabExecutor {

    private static final String PERMISSION = "totemguard.loader.admin";

    private final TGLoaderPaper plugin;
    private final LoaderCommandService service;

    public PaperTgLoaderCommand(TGLoaderPaper plugin) {
        this.plugin = plugin;
        this.service = new LoaderCommandService(plugin);
    }

    private static Component render(LoaderMessage.Line line) {
        Component out = Component.empty();
        for (LoaderMessage.Segment segment : line.segments()) {
            if (segment.color() == LoaderMessage.Color.PREFIX) {
                out = out.append(LoaderPalette.PREFIX);
            } else {
                out = out.append(Component.text(segment.text(), paletteColor(segment.color())));
            }
        }
        return out;
    }

    private static TextColor paletteColor(LoaderMessage.Color color) {
        return switch (color) {
            case PREFIX -> LoaderPalette.BRAND;
            case LABEL -> LoaderPalette.LABEL;
            case VALUE -> LoaderPalette.VALUE;
            case CAPTION -> LoaderPalette.CAPTION;
            case CONNECTIVE -> LoaderPalette.CONNECTIVE;
            case SUCCESS -> LoaderPalette.SUCCESS;
            case DANGER -> LoaderPalette.DANGER;
            case DANGER_SOFT -> LoaderPalette.DANGER_SOFT;
        };
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NonNull [] args) {
        // Console is always allowed. For players we require the permission explicitly,
        // never granting it via `default: op`, because the loader controls which jar
        // runs as TotemGuard and that's too important to hand to every op by default.
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text(
                    "You don't have permission to use /tgloader.", NamedTextColor.RED));
            return true;
        }
        // Sender messaging is scheduled to the main thread so the Sink can be invoked
        // from any background worker without Bukkit complaining.
        LoaderMessage.Sink sink = line -> Bukkit.getScheduler().runTask(plugin,
                () -> sender.sendMessage(render(line)));
        service.dispatch(sink, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return LoaderCommandService.filter(LoaderCommandService.TOP_LEVEL, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "plugin" -> LoaderCommandService.filter(LoaderCommandService.PLUGIN_SUBS, args[1]);
                case "rollout" -> LoaderCommandService.filter(LoaderCommandService.ROLLOUT_SUBS, args[1]);
                case "load", "stage" -> service.versionCandidates(args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rollout")) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("stage") || sub.equals("deploy")) {
                return service.versionCandidates(args[2]);
            }
        }
        return Collections.emptyList();
    }
}
