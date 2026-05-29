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

package com.deathmotion.totemguard.discord.alert;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.event.EventBus;
import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent;
import com.deathmotion.totemguard.api.event.events.TGNetworkAlertEvent;
import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.discord.bot.DiscordBot;
import com.deathmotion.totemguard.discord.config.BotConfig;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class EventBridge {

    private final DiscordBot bot;

    public EventBridge(@NotNull DiscordBot bot) {
        this.bot = bot;
    }

    public void register() {
        subscribe();
    }

    public void unregister() {
        try {
            TotemGuard.get().getEventBus().unregisterAll(this);
        } catch (Exception ignored) {
        }
    }

    private void subscribe() {
        try {
            EventBus bus = TotemGuard.get().getEventBus();
            bus.unregisterAll(this);
            bus.subscribe(TGNetworkAlertEvent.class, this, this::onAlert);
            bus.subscribe(TGDiagnosticEvent.class, this, this::onDiagnostic);
            bus.subscribe(TGPluginShutdownEvent.class, this, this::onShutdown);
        } catch (Exception e) {
            bot.platform().logger().log(Level.WARNING, "Failed to subscribe to TotemGuard events", e);
        }
    }

    private void onShutdown(TGPluginShutdownEvent event) {
        switch (event.getReason()) {
            case LOADER_RESTART, UPDATE_TRIGGERED -> TotemGuard.getAsync().thenAccept(api -> subscribe());
            default -> {
            }
        }
    }

    private void onAlert(TGNetworkAlertEvent event) {
        ShardManager shardManager = bot.shardManager();
        if (shardManager == null) return;
        BotConfig.ChannelConfig channel = event.getKind() == TGNetworkAlertEvent.Kind.PUNISHMENT
                ? bot.config().punishments()
                : bot.config().alerts();
        if (!channel.usable()) return;
        post(shardManager, channel.channelId(), AlertComponentFactory.build(event, channel));
    }

    private void onDiagnostic(TGDiagnosticEvent event) {
        ShardManager shardManager = bot.shardManager();
        if (shardManager == null) return;
        BotConfig.DiagnosticsConfig channel = bot.config().diagnostics();
        if (!channel.usable() || !channel.passes(event.getSeverity())) return;
        post(shardManager, channel.channelId(), DiagnosticComponentFactory.build(event));
    }

    private void post(ShardManager shardManager, long channelId, Container card) {
        bot.worker().execute(() -> {
            try {
                TextChannel target = shardManager.getTextChannelById(channelId);
                if (target == null) {
                    bot.platform().logger().fine("Discord channel " + channelId + " not found or not a text channel.");
                    return;
                }
                target.sendMessageComponents(card).useComponentsV2()
                        .queue(null, error -> bot.platform().logger().log(Level.FINE, "Failed to post to Discord", error));
            } catch (Exception e) {
                bot.platform().logger().log(Level.WARNING, "Failed to build or post Discord message", e);
            }
        });
    }
}
