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

package com.deathmotion.totemguard.discord.command.impl;

import com.deathmotion.totemguard.discord.command.SlashCommand;
import com.deathmotion.totemguard.discord.command.SlashCommandContext;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class ReloadCommand implements SlashCommand {
    @Override
    public @NotNull String name() {
        return "reload";
    }

    @Override
    public @NotNull SubcommandData data() {
        return new SubcommandData("reload", "Reload TotemGuard's configuration.");
    }

    @Override
    public boolean control() {
        return true;
    }

    @Override
    public void handle(@NotNull SlashCommandContext context) {
        context.bot().worker().execute(() -> {
            try {
                context.api().reload();
                context.respond(Cv2.container(Colors.SUCCESS)
                        .heading("Reloaded")
                        .text("TotemGuard configuration has been reloaded.")
                        .build());
            } catch (Exception e) {
                context.bot().platform().logger().log(Level.SEVERE, "Discord reload failed", e);
                context.respondError("Reload failed: " + e.getMessage());
            }
        });
    }
}
