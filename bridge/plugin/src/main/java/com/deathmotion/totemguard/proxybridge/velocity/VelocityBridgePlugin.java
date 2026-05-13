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

package com.deathmotion.totemguard.proxybridge.velocity;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.proxybridge.common.*;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreTransferEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class VelocityBridgePlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;

    private final Set<UUID> transferringPlayers = ConcurrentHashMap.newKeySet();

    private volatile ProxyBridgeCore core;
    private volatile boolean integrityVerified;

    @Inject
    public VelocityBridgePlugin(ProxyServer server, @DataDirectory Path dataFolder) {
        this.server = server;
        this.logger = Logger.getLogger("TotemGuard-Bridge");
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        if (!new JarIntegrityChecker(logger, "TotemGuard-Bridge").verifyCurrentJar()) {
            return;
        }
        this.integrityVerified = true;
        registerCommands();
        bootstrap();
    }

    private void registerCommands() {
        CommandManager commands = server.getCommandManager();
        CommandMeta meta = commands.metaBuilder("tgbridge")
                .aliases("tgbr", "tgpb")
                .plugin(this)
                .build();
        commands.register(meta, new VelocityBridgeCommand(this::bootstrap, logger));
    }

    private synchronized BridgeBootstrap.BootstrapOutcome bootstrap() {
        if (!integrityVerified) {
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.FAILED,
                    "Jar integrity check failed; cannot start.");
        }

        ProxyConfig next;
        try {
            next = VelocityConfigLoader.load(dataFolder, logger);
        } catch (Exception ex) {
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.FAILED,
                    "Failed to load config: " + ex.getMessage());
        }

        if (!next.enabled()) {
            logDisabledBanner();
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.DISABLED,
                    "TotemGuard-Bridge is disabled until 'enabled = true' is set in config.toml.");
        }

        if (core == null) {
            ProxyIdentity identity = ProxyIdentity.fresh();
            ProxyBridgeCore candidate = new ProxyBridgeCore(new VelocityBridgePlatformImpl(), next, identity);
            try {
                candidate.start();
            } catch (Exception ex) {
                try {
                    candidate.stop();
                } catch (Exception ignored) {
                }
                logger.warning("Startup failed: " + ex.getMessage()
                        + ". Fix the config and run /tgbridge reload.");
                return new BridgeBootstrap.BootstrapOutcome(
                        BridgeBootstrap.BootstrapOutcome.Kind.FAILED,
                        "Startup failed: " + ex.getMessage());
            }
            core = candidate;
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.STARTED,
                    "TotemGuard-Bridge online.");
        }

        core.reload(next);
        return new BridgeBootstrap.BootstrapOutcome(
                BridgeBootstrap.BootstrapOutcome.Kind.RELOADED,
                "TotemGuard-Bridge reloaded.");
    }

    private void logDisabledBanner() {
        Path file = dataFolder.resolve("config.toml");
        logger.warning("================================================================");
        logger.warning(" TotemGuard-Bridge: configuration required");
        logger.warning("----------------------------------------------------------------");
        logger.warning(" The bridge is currently DISABLED and will not connect to Redis.");
        logger.warning(" To finish setup:");
        logger.warning("   1. Open: " + file);
        logger.warning("   2. Set the [redis] section to match your TotemGuard backends.");
        logger.warning("   3. Change 'enabled = false' to 'enabled = true'.");
        logger.warning("   4. Run: /tgbridge reload   (or restart the proxy)");
        logger.warning("================================================================");
    }

    @Subscribe
    public void onReload(ProxyReloadEvent event) {
        if (core != null) core.refreshBackends();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) core.stop();
    }

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        transferringPlayers.remove(uuid);
        if (core == null) return;
        core.onPlayerJoin(uuid);
    }

    @Subscribe
    public void onSwitch(ServerPostConnectEvent event) {
        if (core == null) return;
        String destinationSlot = event.getPlayer().getCurrentServer()
                .map(c -> c.getServerInfo().getName())
                .orElse(null);
        core.onPlayerSwitch(event.getPlayer().getUniqueId(), destinationSlot);
    }

    @Subscribe
    public void onTransfer(PreTransferEvent event) {
        transferringPlayers.add(event.player().getUniqueId());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (core == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        boolean transferred = transferringPlayers.remove(uuid);
        if (transferred) {
            core.onPlayerTransfer(uuid);
        } else {
            core.onPlayerDisconnect(uuid);
        }
    }

    private final class VelocityBridgePlatformImpl implements BridgePlatform {

        @Override
        public @NonNull Kind kind() {
            return Kind.VELOCITY;
        }

        @Override
        public java.util.logging.@NonNull Logger logger() {
            return logger;
        }

        @Override
        public @NonNull Set<String> registeredBackendNames() {
            return server.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public @NonNull Map<String, InetSocketAddress> registeredBackends() {
            Map<String, InetSocketAddress> out = new LinkedHashMap<>();
            for (RegisteredServer s : server.getAllServers()) {
                out.put(s.getServerInfo().getName(), s.getServerInfo().getAddress());
            }
            return out;
        }

        @Override
        public void connect(@NonNull UUID playerUuid, @NonNull String targetBackend) {
            Optional<Player> player = server.getPlayer(playerUuid);
            Optional<RegisteredServer> target = server.getServer(targetBackend);
            if (player.isEmpty() || target.isEmpty()) return;
            player.get().createConnectionRequest(target.get()).fireAndForget();
        }

        @Override
        public void scheduleRepeating(@NonNull Runnable task, long delay, long period, @NonNull TimeUnit unit) {
            server.getScheduler().buildTask(VelocityBridgePlugin.this, task)
                    .delay(delay, unit)
                    .repeat(period, unit)
                    .schedule();
        }
    }
}
