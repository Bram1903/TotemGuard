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

package com.deathmotion.totemguard.proxybridge.bungee;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.proxybridge.common.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class BungeeBridgePlugin extends Plugin implements Listener {

    private volatile ProxyBridgeCore core;
    private volatile boolean integrityVerified;

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        if (!new JarIntegrityChecker(logger, "TotemGuard-Bridge").verifyCurrentJar()) {
            return;
        }
        this.integrityVerified = true;
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this,
                new BungeeBridgeCommand(this::bootstrap, logger));
        bootstrap();
    }

    @Override
    public void onDisable() {
        if (core != null) core.stop();
    }

    private synchronized BridgeBootstrap.BootstrapOutcome bootstrap() {
        Logger logger = getLogger();
        if (!integrityVerified) {
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.FAILED,
                    "Jar integrity check failed; cannot start.");
        }

        ProxyConfig next;
        try {
            next = BungeeConfigLoader.load(getDataFolder().toPath(), logger);
        } catch (Exception ex) {
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.FAILED,
                    "Failed to load config: " + ex.getMessage());
        }

        if (!next.enabled()) {
            logDisabledBanner();
            return new BridgeBootstrap.BootstrapOutcome(
                    BridgeBootstrap.BootstrapOutcome.Kind.DISABLED,
                    "TotemGuard-Bridge is disabled until 'enabled: true' is set in config.yml.");
        }

        if (core == null) {
            ProxyIdentity identity = ProxyIdentity.fresh();
            ProxyBridgeCore candidate = new ProxyBridgeCore(new BungeeBridgePlatformImpl(logger), next, identity);
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
        Logger logger = getLogger();
        Path file = getDataFolder().toPath().resolve("config.yml");
        logger.warning("================================================================");
        logger.warning(" TotemGuard-Bridge: configuration required");
        logger.warning("----------------------------------------------------------------");
        logger.warning(" The bridge is currently DISABLED and will not connect to Redis.");
        logger.warning(" To finish setup:");
        logger.warning("   1. Open: " + file);
        logger.warning("   2. Set the 'redis' section to match your TotemGuard backends.");
        logger.warning("   3. Change 'enabled: false' to 'enabled: true'.");
        logger.warning("   4. Run: /tgbridge reload   (or restart the proxy)");
        logger.warning("================================================================");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PostLoginEvent event) {
        if (core == null) return;
        core.onPlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwitch(ServerSwitchEvent event) {
        if (core == null) return;
        core.onPlayerSwitch(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerDisconnectEvent event) {
        if (core == null) return;
        core.onPlayerQuit(event.getPlayer().getUniqueId());
    }

    private final class BungeeBridgePlatformImpl implements BridgePlatform {

        private final Logger logger;

        BungeeBridgePlatformImpl(Logger logger) {
            this.logger = logger;
        }

        @Override
        public @NonNull Kind kind() {
            return Kind.BUNGEECORD;
        }

        @Override
        public @NonNull Logger logger() {
            return logger;
        }

        @Override
        public @NonNull Set<String> registeredBackendNames() {
            return ProxyServer.getInstance().getServers().keySet().stream()
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public @NonNull Map<String, InetSocketAddress> registeredBackends() {
            Map<String, InetSocketAddress> out = new LinkedHashMap<>();
            for (Map.Entry<String, ServerInfo> entry : ProxyServer.getInstance().getServers().entrySet()) {
                SocketAddress addr = entry.getValue().getSocketAddress();
                if (addr instanceof InetSocketAddress inet) out.put(entry.getKey(), inet);
            }
            return out;
        }

        @Override
        public void connect(@NonNull UUID playerUuid, @NonNull String targetBackend) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerUuid);
            if (player == null) return;
            ServerInfo target = ProxyServer.getInstance().getServerInfo(targetBackend);
            if (target == null) return;
            player.connect(target);
        }

        @Override
        public void scheduleRepeating(@NonNull Runnable task, long delay, long period, @NonNull TimeUnit unit) {
            ProxyServer.getInstance().getScheduler()
                    .schedule(BungeeBridgePlugin.this, task, delay, period, unit);
        }
    }
}
