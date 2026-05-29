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

package com.deathmotion.totemguard.discord.bot.presence;

import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.discord.bot.DiscordBot;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class PresenceService {
    private final DiscordBot bot;
    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> task;

    public PresenceService(@NotNull DiscordBot bot, @NotNull ScheduledExecutorService scheduler) {
        this.bot = bot;
        this.scheduler = scheduler;
    }

    private static Activity activityFor(String type, String text) {
        if (text.isBlank()) return null;
        return switch (type.toUpperCase()) {
            case "PLAYING" -> Activity.playing(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            default -> Activity.watching(text);
        };
    }

    public void start() {
        int interval = bot.config().status().updateIntervalSeconds();
        this.task = scheduler.scheduleAtFixedRate(this::update, 10, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        ScheduledFuture<?> current = this.task;
        this.task = null;
        if (current != null) current.cancel(false);
    }

    private void update() {
        ShardManager shardManager = bot.shardManager();
        if (shardManager == null) return;
        try {
            NetworkRepository network = bot.api().getNetworkRepository();
            String text = bot.config().status().text()
                    .replace("%players%", Integer.toString(network.getTrackedPlayerCount()))
                    .replace("%servers%", Integer.toString(network.getConnectedServerCount()));

            Activity activity = activityFor(bot.config().status().activity(), text);
            if (activity != null) shardManager.setActivity(activity);
        } catch (Exception e) {
            bot.platform().logger().log(Level.FINE, "Failed to update Discord presence", e);
        }
    }
}
