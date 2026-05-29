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

import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.discord.command.SlashCommand;
import com.deathmotion.totemguard.discord.command.SlashCommandContext;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

public final class VersionCommand implements SlashCommand {
    @Override
    public @NotNull String name() {
        return "version";
    }

    @Override
    public @NotNull SubcommandData data() {
        return new SubcommandData("version", "Show the running TotemGuard version.");
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public void handle(@NotNull SlashCommandContext context) {
        TotemGuardAPI api = context.api();
        Cv2 container = Cv2.container(Colors.BRAND)
                .heading("TotemGuard")
                .text("**Plugin version:** `" + api.getVersion().toDisplayString() + "`")
                .text("**API version:** `" + api.getApiVersion().toDisplayString() + "`");

        api.getLoaderInfo().ifPresent(info -> {
            container.divider()
                    .text("**Loader:** `" + info.loaderVersion() + "`")
                    .text("**Source:** `" + info.configuredSource() + "` • **Pin:** `" + info.configuredVersion() + "`");
            if (info.stagedVersion() != null) {
                container.text("**Staged update:** `" + info.stagedVersion() + "`");
            }
        });

        context.respond(container.build());
    }
}
