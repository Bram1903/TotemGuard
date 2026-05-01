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
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.punishment.BanAnimation;
import com.deathmotion.totemguard.common.util.Palette;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class TestBanAnimationCommand extends AbstractCommand {

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("testban")
                        .permission(perm("debug"))
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        TGPlayer player = sender.getTGPlayer();
        if (player == null) {
            sender.sendMessage(Component.text("Your player data could not be found in the player repository", Palette.DANGER));
            return;
        }

        if (!BanAnimation.isSupported(player)) {
            sender.sendMessage(Component.text("Ban animation requires a 1.21.2+ client.", Palette.DANGER));
            return;
        }

        BanAnimation.play(player);
        sender.sendMessage(Component.text("Played ban animation.", Palette.BRAND));

        TGPlatform.getInstance().getScheduler().runAsyncTaskDelayed(
                () -> player.disconnect("Test ban animation"),
                BanAnimation.ANIMATION_DURATION_MS,
                TimeUnit.MILLISECONDS
        );
    }
}
