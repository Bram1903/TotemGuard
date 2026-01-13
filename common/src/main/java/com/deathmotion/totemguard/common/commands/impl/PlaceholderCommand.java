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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlaceholderCommand extends AbstractCommand {

    private final PlaceholderRepositoryImpl placeholderRepository;

    public PlaceholderCommand() {
        this.placeholderRepository = TGPlatform.getInstance().getPlaceholderRepository();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("placeholder")
                        .permission(perm("placeholder"))
                        .handler(this::executePlaceholderCommand)
        );

        manager.command(
                base(manager)
                        .literal("placeholder")
                        .literal("list")
                        .permission(perm("placeholder"))
                        .handler(this::executeList)
        );

        manager.command(
                base(manager)
                        .literal("placeholder")
                        .literal("parse")
                        .required("message", StringParser.greedyStringParser())
                        .permission(perm("placeholder"))
                        .handler(this::executeParse)
        );
    }

    private void executePlaceholderCommand(@NonNull CommandContext<Sender> context) {
        Component msg = Component.empty()
                .append(Component.text("TotemGuard Placeholder Tools", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Usage:", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("  /tg placeholder list", NamedTextColor.YELLOW))
                .append(Component.text(" - show all registered placeholder keys", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("  /tg placeholder parse <message>", NamedTextColor.YELLOW))
                .append(Component.text(" - parse placeholders in a message", NamedTextColor.GRAY));

        context.sender().sendMessage(msg);
    }

    private void executeList(@NonNull CommandContext<Sender> context) {
        Set<String> keys = placeholderRepository.registeredKeys();

        if (keys.isEmpty()) {
            context.sender().sendMessage(Component.text("No placeholder providers are registered.", NamedTextColor.RED));
            return;
        }

        final int pageSize = 40;
        List<String> all = new ArrayList<>(keys);
        int pages = (all.size() + pageSize - 1) / pageSize;

        int page = 1;

        int from = 0;
        int to = Math.min(from + pageSize, all.size());

        Component header = Component.empty()
                .append(Component.text("Registered Placeholders", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" (" + all.size() + ")", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("Showing page " + page + "/" + pages, NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("────────────────────────────", NamedTextColor.DARK_GRAY))
                .append(Component.newline());

        Component body = Component.empty();
        for (int i = from; i < to; i++) {
            String key = all.get(i);
            body = body.append(Component.text("%" + key + "%", NamedTextColor.YELLOW))
                    .append(Component.text("  ", NamedTextColor.DARK_GRAY));
            if ((i - from + 1) % 4 == 0) body = body.append(Component.newline());
        }

        context.sender().sendMessage(header.append(body));
    }

    private void executeParse(@NonNull CommandContext<Sender> context) {
        String message = context.get("message");

        TGPlayer player = null;
        if (context.sender().isPlayer()) {
            player = TGPlatform.getInstance()
                    .getPlayerRepository()
                    .getPlayer(context.sender().getUniqueId());
        }

        String parsed = (player != null)
                ? placeholderRepository.replace(message, player, null)
                : placeholderRepository.replace(message);

        Component msg = Component.empty()
                .append(Component.text("Placeholder Parse", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Input:  ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Output: ", NamedTextColor.GRAY))
                .append(Component.text(parsed, NamedTextColor.WHITE));

        context.sender().sendMessage(msg);
    }
}
