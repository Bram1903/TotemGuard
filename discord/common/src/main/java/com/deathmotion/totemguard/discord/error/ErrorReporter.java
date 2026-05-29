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

package com.deathmotion.totemguard.discord.error;

import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent.Severity;
import com.deathmotion.totemguard.discord.bot.DiscordBot;
import com.deathmotion.totemguard.discord.config.BotConfig;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import com.deathmotion.totemguard.discord.ui.Format;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

public final class ErrorReporter {

    private final DiscordBot bot;

    public ErrorReporter(@NotNull DiscordBot bot) {
        this.bot = bot;
    }

    private static String render(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public void report(@NotNull String context, @NotNull Throwable error) {
        bot.platform().logger().log(Level.SEVERE, "Discord bot error: " + context, error);
        postToChannel(context, error);
    }

    private void postToChannel(String context, Throwable error) {
        BotConfig.DiagnosticsConfig channel = bot.config().diagnostics();
        if (!channel.usable() || !channel.passes(Severity.ERROR)) return;
        ShardManager shardManager = bot.shardManager();
        if (shardManager == null) return;

        bot.worker().execute(() -> {
            try {
                TextChannel target = shardManager.getTextChannelById(channel.channelId());
                if (target == null) return;
                Container card = Cv2.container(Colors.ERROR)
                        .heading("Bot error")
                        .text(context)
                        .divider()
                        .codeBlock(render(error))
                        .subtle(Format.dateTime(System.currentTimeMillis()))
                        .build();
                target.sendMessageComponents(card).useComponentsV2().queue(null, ignored -> {
                });
            } catch (Exception e) {
                bot.platform().logger().log(Level.FINE, "Failed to post bot error to the diagnostics channel", e);
            }
        });
    }
}
