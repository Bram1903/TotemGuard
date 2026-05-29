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

import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.discord.command.SlashCommand;
import com.deathmotion.totemguard.discord.command.SlashCommandContext;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

public final class StatusCommand implements SlashCommand {
    @Override
    public @NotNull String name() {
        return "status";
    }

    @Override
    public @NotNull SubcommandData data() {
        return new SubcommandData("status", "Live anticheat status across the network.");
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public void handle(@NotNull SlashCommandContext context) {
        NetworkRepository network = context.api().getNetworkRepository();

        Cv2 container = Cv2.container(Colors.BRAND)
                .heading("TotemGuard Status")
                .text("**Players being checked (network):** `" + network.getTrackedPlayerCount() + "`")
                .text("**Servers connected:** `" + network.getConnectedServerCount() + "`")
                .text("**Local server:** `" + network.getLocalServerName() + "`")
                .text("**Redis:** " + (network.isConnected() ? "connected" : "offline"));

        ShardManager shardManager = context.bot().shardManager();
        if (shardManager != null) {
            container.divider().subtle("Shards " + shardManager.getShardsRunning() + "/" + shardManager.getShardsTotal());
        }

        context.respond(container.build());
    }
}
