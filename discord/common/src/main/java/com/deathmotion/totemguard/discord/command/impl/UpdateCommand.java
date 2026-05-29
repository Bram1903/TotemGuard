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

import com.deathmotion.totemguard.api.loader.LoaderControl;
import com.deathmotion.totemguard.discord.command.SlashCommand;
import com.deathmotion.totemguard.discord.command.SlashCommandContext;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;

public final class UpdateCommand implements SlashCommand {
    private static String detail(String detail, String fallback) {
        return detail != null ? detail : fallback;
    }

    @Override
    public @NotNull String name() {
        return "update";
    }

    @Override
    public @NotNull SubcommandData data() {
        return new SubcommandData("update", "Check for and apply a TotemGuard update via the loader.");
    }

    @Override
    public boolean control() {
        return true;
    }

    @Override
    public void handle(@NotNull SlashCommandContext context) {
        Optional<LoaderControl> control = context.api().getLoaderControl();
        if (control.isEmpty()) {
            context.respondError("Updates are only available when TotemGuard runs under the loader.");
            return;
        }

        context.respond(Cv2.container(Colors.BRAND)
                .heading("Updating")
                .text("Checking for the latest version...")
                .build());

        control.get().update().whenComplete((result, error) -> {
            if (error != null || result == null) {
                context.bot().platform().logger().log(Level.WARNING, "Discord-triggered update failed", error);
                context.respondError("Update failed: " + (error != null ? error.getMessage() : "unknown error"));
                return;
            }
            switch (result.outcome()) {
                case UP_TO_DATE -> context.respond(Cv2.container(Colors.SUCCESS)
                        .heading("Up to date")
                        .text(detail(result.detail(), "Already running the latest version."))
                        .build());
                case UPDATING -> context.respond(Cv2.container(Colors.SUCCESS)
                        .heading("Update applied")
                        .text(detail(result.detail(), "Update staged; TotemGuard is restarting."))
                        .build());
                case FAILED -> context.respondError(detail(result.detail(), "Update failed."));
            }
        });
    }
}
