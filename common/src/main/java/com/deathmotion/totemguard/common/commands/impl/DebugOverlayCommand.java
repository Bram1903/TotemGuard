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

import com.deathmotion.totemguard.api3.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.util.Palette;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DebugOverlayCommand extends AbstractCommand {

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("debug")
                        .permission(perm("debug"))
                        .handler(this::showHelp)
        );

        manager.command(
                base(manager)
                        .literal("debug")
                        .required(
                                "overlay",
                                StringParser.stringParser(),
                                SuggestionProvider.blockingStrings((ctx, input) -> overlaySuggestions(ctx.sender(), input.lastRemainingToken()))
                        )
                        .permission(perm("debug"))
                        .handler(this::toggleOverlay)
        );
    }

    private void showHelp(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        TGPlayer player = sender.getTGPlayer();
        if (player == null) {
            sender.sendMessage(TGPlatform.getInstance().getMessageService().getComponent(MessagesKeys.GENERAL_PLAYER_DATA_MISSING));
            return;
        }

        String overlays = player.getDebugOverlayManager().registeredKeys().stream()
                .sorted()
                .collect(Collectors.joining(", "));

        Component message = Component.empty()
                .append(Component.text("Debug Overlays", Palette.BRAND, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Use /tg debug <overlay> to toggle one.", Palette.CONNECTIVE))
                .append(Component.newline())
                .append(Component.text("Available: ", Palette.LABEL))
                .append(Component.text(overlays, Palette.VALUE));

        String activeOverlay = player.getDebugOverlayManager().getActiveOverlayKey();
        if (activeOverlay != null) {
            message = message.append(Component.newline())
                    .append(Component.text("Active: ", Palette.LABEL))
                    .append(Component.text(activeOverlay, Palette.VALUE));
        }

        sender.sendMessage(message);
    }

    private Iterable<String> overlaySuggestions(Sender sender, String currentInput) {
        if (!sender.isPlayer()) {
            return List.of();
        }

        TGPlayer player = sender.getTGPlayer();
        if (player == null) {
            return List.of();
        }

        String normalizedInput = currentInput.toLowerCase();
        List<String> suggestions = new ArrayList<>();

        for (String key : player.getDebugOverlayManager().registeredKeys()) {
            DebugOverlayProvider provider = player.getDebugOverlayManager().getProvider(key);
            if (provider == null) {
                continue;
            }

            if (!sender.hasPermission(perm(provider.getPermissionSuffix()))) {
                continue;
            }

            if (!key.startsWith(normalizedInput)) {
                continue;
            }

            suggestions.add(key);
        }

        suggestions.sort(String::compareTo);
        return suggestions;
    }

    private void toggleOverlay(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        TGPlayer player = sender.getTGPlayer();
        if (player == null) {
            sender.sendMessage(TGPlatform.getInstance().getMessageService().getComponent(MessagesKeys.GENERAL_PLAYER_DATA_MISSING));
            return;
        }

        String overlayKey = context.get("overlay");
        MessageService messages = TGPlatform.getInstance().getMessageService();
        DebugOverlayProvider provider = player.getDebugOverlayManager().getProvider(overlayKey);
        if (provider == null) {
            sender.sendMessage(messages.getComponent(
                    MessagesKeys.DEBUG_UNKNOWN_OVERLAY,
                    Map.of("tg_overlay", overlayKey)
            ));
            return;
        }

        if (!sender.hasPermission(perm(provider.getPermissionSuffix()))) {
            sender.sendMessage(messages.getComponent(MessagesKeys.DEBUG_NO_PERMISSION));
            return;
        }

        boolean enabled = player.getDebugOverlayManager().toggle(provider.getKey());
        sender.sendMessage(messages.getComponent(
                enabled ? MessagesKeys.DEBUG_ENABLED : MessagesKeys.DEBUG_DISABLED,
                Map.of("tg_overlay_name", provider.getDisplayName())
        ));
    }
}
