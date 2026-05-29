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

package com.deathmotion.totemguard.discord.bot;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.cluster.ClusterLease;
import com.deathmotion.totemguard.discord.DiscordPlatform;
import com.deathmotion.totemguard.discord.alert.EventBridge;
import com.deathmotion.totemguard.discord.bot.presence.PresenceService;
import com.deathmotion.totemguard.discord.command.SlashCommandListener;
import com.deathmotion.totemguard.discord.command.SlashCommandManager;
import com.deathmotion.totemguard.discord.config.BotConfig;
import com.deathmotion.totemguard.discord.error.ErrorReporter;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class DiscordBot {
    private static final String LEASE_NAME = "discord-bot";
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final long TICK_SECONDS = 15;

    private final DiscordPlatform platform;
    private final BotConfig config;
    private final ErrorReporter errorReporter;

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(3, named("totemguard-discord"));

    private volatile boolean stopped;
    private volatile ScheduledFuture<?> connectionTask;
    private volatile ClusterLease lease;

    private volatile ShardManager shardManager;
    private volatile PresenceService presenceService;
    private volatile EventBridge eventBridge;

    public DiscordBot(@NotNull DiscordPlatform platform, @NotNull BotConfig config) {
        this.platform = platform;
        this.config = config;
        this.errorReporter = new ErrorReporter(this);
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public void start() {
        this.eventBridge = new EventBridge(this);
        this.eventBridge.register();
        this.connectionTask = executor.scheduleAtFixedRate(this::maintainConnection, 0, TICK_SECONDS, TimeUnit.SECONDS);
    }

    public void shutdown() {
        stopped = true;
        ScheduledFuture<?> task = this.connectionTask;
        this.connectionTask = null;
        if (task != null) task.cancel(false);

        EventBridge bridge = this.eventBridge;
        this.eventBridge = null;
        if (bridge != null) {
            try {
                bridge.unregister();
            } catch (Exception e) {
                platform.logger().log(Level.WARNING, "Failed to unregister Discord event bridge", e);
            }
        }

        closeConnection();

        ClusterLease held = this.lease;
        this.lease = null;
        if (held != null) {
            try {
                held.release();
            } catch (Exception ignored) {
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void maintainConnection() {
        if (stopped) return;
        try {
            if (shardManager != null) {
                ClusterLease current = this.lease;
                if (current == null || !current.renew(LEASE_TTL)) {
                    platform.logger().info("Lost the Discord connection lock, disconnecting this node.");
                    closeConnection();
                    this.lease = null;
                }
                return;
            }

            Optional<ClusterLease> acquired = TotemGuard.get().getCluster().acquireLease(LEASE_NAME, LEASE_TTL);
            if (acquired.isPresent()) {
                this.lease = acquired.get();
                openConnection();
            }
        } catch (Exception e) {
            platform.logger().log(Level.FINE, "Discord connection attempt failed, will retry", e);
        }
    }

    private void openConnection() {
        SlashCommandManager commandManager = new SlashCommandManager(this);
        SlashCommandListener commandListener = new SlashCommandListener(commandManager, platform.logger());

        BotConfig.StatusConfig status = config.status();
        Activity initialActivity = status.enabled()
                ? JdaFactory.activity(status.activity(), status.text().replace("%players%", "0").replace("%servers%", "1"))
                : null;

        this.shardManager = JdaFactory.create(config, initialActivity, commandListener);

        if (status.enabled()) {
            this.presenceService = new PresenceService(this, executor);
            this.presenceService.start();
        }

        if (TotemGuard.get().getCluster().isConnected()) {
            platform.logger().info("This node now holds the Discord connection, connecting (auto-sharding).");
        } else {
            platform.logger().info("Redis is not connected, so the Discord bot assumes this is the only TotemGuard node. "
                    + "If you run it on multiple backends, enable Redis so they share a single bot connection.");
        }
    }

    private void closeConnection() {
        PresenceService presence = this.presenceService;
        this.presenceService = null;
        if (presence != null) presence.stop();

        ShardManager manager = this.shardManager;
        this.shardManager = null;
        if (manager != null) {
            try {
                manager.shutdown();
            } catch (Exception e) {
                platform.logger().log(Level.WARNING, "Error during JDA shutdown", e);
            }
        }
    }

    public @NotNull DiscordPlatform platform() {
        return platform;
    }

    public @NotNull TotemGuardAPI api() {
        return TotemGuard.get();
    }

    public @NotNull BotConfig config() {
        return config;
    }

    public @NotNull ErrorReporter errorReporter() {
        return errorReporter;
    }

    public @Nullable ShardManager shardManager() {
        return shardManager;
    }

    public @NotNull ScheduledExecutorService worker() {
        return executor;
    }
}
