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

package com.deathmotion.totemguard.discord;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.discord.bot.DiscordBot;
import com.deathmotion.totemguard.discord.config.BotConfig;
import com.deathmotion.totemguard.discord.config.ConfigLoader;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class DiscordService {
    private static final long API_WAIT_SECONDS = 60;

    private final DiscordPlatform platform;
    private final ExecutorService lifecycle = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "totemguard-discord-init");
        thread.setDaemon(true);
        return thread;
    });

    private volatile DiscordBot bot;
    private volatile boolean stopped;

    public DiscordService(@NotNull DiscordPlatform platform) {
        this.platform = platform;
    }

    public void start() {
        lifecycle.execute(() -> launch(false));
    }

    public void reload() {
        lifecycle.execute(() -> {
            platform.logger().info("Reloading the TotemGuard Discord bot...");
            shutdownBot();
            launch(true);
        });
    }

    public void stop() {
        stopped = true;
        shutdownBot();
        lifecycle.shutdownNow();
    }

    private void launch(boolean reload) {
        if (stopped) return;

        BotConfig config;
        try {
            config = ConfigLoader.load(platform.dataDirectory());
        } catch (Exception e) {
            platform.logger().log(Level.SEVERE, "Failed to load the Discord bot config.", e);
            return;
        }

        if (!config.botEnabled()) {
            platform.logger().info("TotemGuard Discord bot is disabled (no token in config.yml). "
                    + "Add a bot token and run '/tgdiscord reload' to enable it without restarting the server.");
            return;
        }

        if (!reload) {
            platform.logger().info("Waiting for the TotemGuard API before starting the Discord bot...");
        }

        TotemGuardAPI api;
        try {
            api = TotemGuard.getAsync().get(API_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            platform.logger().log(Level.SEVERE, "TotemGuard API unavailable, Discord bot not started.", e);
            return;
        }

        if (stopped || api == null) return;

        try {
            DiscordBot started = new DiscordBot(platform, config);
            started.start();
            this.bot = started;
            if (stopped) {
                this.bot = null;
                started.shutdown();
                return;
            }
            platform.logger().info(reload ? "TotemGuard Discord bot reloaded." : "TotemGuard Discord bot started.");
        } catch (Throwable t) {
            platform.logger().log(Level.SEVERE, "Failed to start the Discord bot.", t);
        }
    }

    private void shutdownBot() {
        DiscordBot current = this.bot;
        this.bot = null;
        if (current != null) {
            try {
                current.shutdown();
            } catch (Exception e) {
                platform.logger().log(Level.WARNING, "Error while shutting down the Discord bot", e);
            }
        }
    }
}
