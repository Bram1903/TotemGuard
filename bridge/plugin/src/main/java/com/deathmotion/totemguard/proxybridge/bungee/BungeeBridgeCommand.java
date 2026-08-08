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

package com.deathmotion.totemguard.proxybridge.bungee;

import com.deathmotion.totemguard.proxybridge.common.BridgeBootstrap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

final class BungeeBridgeCommand extends Command implements TabExecutor {

    static final String PERMISSION = "totemguard.proxybridge.reload";

    private final BridgeBootstrap bootstrap;
    private final Logger logger;

    BungeeBridgeCommand(@NonNull BridgeBootstrap bootstrap, @NonNull Logger logger) {
        super("tgbridge", PERMISSION, "tgbr", "tgpb");
        this.bootstrap = bootstrap;
        this.logger = logger;
    }

    private static ChatColor colorFor(BridgeBootstrap.BootstrapOutcome.Kind kind) {
        return switch (kind) {
            case STARTED, RELOADED -> ChatColor.GREEN;
            case DISABLED -> ChatColor.YELLOW;
            case FAILED -> ChatColor.RED;
        };
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission."));
            return;
        }

        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /tgbridge reload"));
            return;
        }

        try {
            BridgeBootstrap.BootstrapOutcome outcome = bootstrap.reload();
            sender.sendMessage(new TextComponent(colorFor(outcome.kind()) + outcome.message()));
        } catch (Exception ex) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Reload failed: " + ex.getMessage()));
            logger.warning("Reload failed: " + ex.getMessage());
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return Collections.emptyList();
        if (args.length <= 1) return List.of("reload");
        return Collections.emptyList();
    }
}
