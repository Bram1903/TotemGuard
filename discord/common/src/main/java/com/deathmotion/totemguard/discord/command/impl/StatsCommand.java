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

import com.deathmotion.totemguard.api.result.Result;
import com.deathmotion.totemguard.api.stats.StatsSnapshot;
import com.deathmotion.totemguard.api.stats.StatsWindow;
import com.deathmotion.totemguard.discord.command.SlashCommand;
import com.deathmotion.totemguard.discord.command.SlashCommandContext;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public final class StatsCommand implements SlashCommand {
    private static StatsWindow window(@Nullable String raw) {
        if (raw == null) return StatsWindow.ALL_TIME;
        return switch (raw) {
            case "30d" -> StatsWindow.LAST_30_DAYS;
            case "7d" -> StatsWindow.LAST_7_DAYS;
            case "24h" -> StatsWindow.LAST_24_HOURS;
            default -> StatsWindow.ALL_TIME;
        };
    }

    private static String label(StatsWindow window) {
        return switch (window) {
            case LAST_30_DAYS -> "last 30 days";
            case LAST_7_DAYS -> "last 7 days";
            case LAST_24_HOURS -> "last 24 hours";
            default -> "all time";
        };
    }

    private static String message(@Nullable Result<StatsSnapshot> result) {
        if (result != null && result.message() != null) return result.message();
        return "Statistics are currently unavailable.";
    }

    @Override
    public @NotNull String name() {
        return "stats";
    }

    @Override
    public @NotNull SubcommandData data() {
        return new SubcommandData("stats", "Aggregated alert and punishment statistics.")
                .addOptions(new OptionData(OptionType.STRING, "window", "Time window (default: all time)", false)
                        .addChoice("All time", "all")
                        .addChoice("Last 30 days", "30d")
                        .addChoice("Last 7 days", "7d")
                        .addChoice("Last 24 hours", "24h"));
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public void handle(@NotNull SlashCommandContext context) {
        StatsWindow window = window(context.optionString("window"));
        context.api().getStatsRepository().snapshot(window).whenComplete((result, error) -> {
            if (error != null) {
                context.bot().platform().logger().log(Level.WARNING, "Discord stats query failed", error);
                context.respondError("Failed to query statistics.");
                return;
            }
            if (result == null || !result.ok() || result.value() == null) {
                context.respondError(message(result));
                return;
            }
            StatsSnapshot stats = result.value();
            context.respond(Cv2.container(Colors.BRAND)
                    .heading("Statistics (" + label(window) + ")")
                    .text("**Alerts:** `" + stats.alertCount() + "`")
                    .text("**Punishments:** `" + stats.punishmentCount() + "`")
                    .text("**Unique players:** `" + stats.uniquePlayers() + "`")
                    .text("**Flagged players:** `" + stats.flaggedPlayers() + "`")
                    .text("**Punished players:** `" + stats.punishedPlayers() + "`")
                    .build());
        });
    }
}
