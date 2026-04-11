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
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.gui.screen.PlayerProfileScreen;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

public final class ProfileCommand extends AbstractCommand {

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("profile")
                        .required(
                                "tg_player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProvider()
                        )
                        .permission(perm("gui.profile"))
                        .handler(this::openProfileGui)
        );
    }

    private void openProfileGui(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        TGPlayer target = resolveTarget(sender, context.get("tg_player"));
        if (target == null) {
            return;
        }

        if (!TGPlatform.getInstance().getGuiManager().open(sender, new PlayerProfileScreen(target))) {
            sender.sendMessage(Component.text("Failed to open the profile GUI.", NamedTextColor.RED));
        }
    }

    private TGPlayer resolveTarget(Sender sender, String rawTarget) {
        TGPlayer target = TGPlayerSuggestionProvider.findPlayer(rawTarget);
        if (target != null) {
            return target;
        }

        sender.sendMessage(Component.text(
                "Could not find a tracked TotemGuard player named '" + rawTarget + "'.",
                NamedTextColor.RED
        ));
        return null;
    }
}
