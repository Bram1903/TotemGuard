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

package com.deathmotion.totemguard.velocity;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.velocity.player.VelocityPlatformUserFactory;
import com.deathmotion.totemguard.velocity.sender.VelocitySenderFactory;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;

import java.nio.file.Path;

@Getter
public class TGVelocity {

    private final ProxyServer server;
    private final PluginContainer pluginContainer;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    private final TGVelocityPlatform tg;
    private final VelocitySenderFactory senderFactory;
    private final VelocityPlatformUserFactory platformUserFactory;
    private CommandManager<Sender> commandManager;

    @Inject
    public TGVelocity(ProxyServer server, PluginContainer pluginContainer, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.pluginContainer = pluginContainer;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;

        this.tg = new TGVelocityPlatform(this);
        this.senderFactory = new VelocitySenderFactory();
        this.platformUserFactory = new VelocityPlatformUserFactory(server);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent ignoredEvent) {
        tg.commonOnInitialize();
        tg.commonOnEnable();
        enableBStats();
    }

    @Subscribe()
    public void onProxyShutdown(ProxyShutdownEvent ignoredEvent) {
        tg.commonOnDisable();
    }

    CommandManager<Sender> getCommandManager() {
        if (commandManager == null) {
            commandManager = new VelocityCommandManager<>(
                    pluginContainer,
                    server,
                    ExecutionCoordinator.simpleCoordinator(),
                    senderFactory
            );
        }

        return commandManager;
    }

    void enableBStats() {
        try {
            Metrics metrics = metricsFactory.make(this, TGPlatform.getBStatsId());
            metrics.addCustomChart(new SimplePie("tg_version", TGVersions.CURRENT::toStringWithoutSnapshot));
            metrics.addCustomChart(new SimplePie("tg_platform", () -> "Velocity"));
        } catch (Exception e) {
            tg.getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
        }
    }
}