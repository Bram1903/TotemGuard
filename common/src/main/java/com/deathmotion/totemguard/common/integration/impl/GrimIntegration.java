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

package com.deathmotion.totemguard.common.integration.impl;

import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.events.GrimTeleportEvent;
import ac.grim.grimac.api.event.events.GrimTransactionReceivedEvent;
import ac.grim.grimac.api.plugin.BasicGrimPlugin;
import ac.grim.grimac.api.plugin.GrimPlugin;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.integration.Integration;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.TGVersions;

import java.io.File;
import java.util.List;

public final class GrimIntegration implements Integration {

    private static final String PLUGIN_NAME = "GrimAC";

    private final PlayerRepositoryImpl playerRepository = TGPlatform.getInstance().getPlayerRepository();

    private boolean enabled;
    private EventBus eventBus;
    private GrimPlugin grimPlugin;

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        try {
            eventBus = GrimAPIProvider.get().getEventBus();
            grimPlugin = createPluginContext();
            registerListeners();

            enabled = true;
            TGPlatform.getInstance().getLogger().info(PLUGIN_NAME + " detected, hooking into the API for compatibility.");
        } catch (Exception | LinkageError exception) {
            enabled = false;
            eventBus = null;
            grimPlugin = null;
            TGPlatform.getInstance().getLogger().severe("Failed to enable " + PLUGIN_NAME + " integration: " + exception);
        }
    }

    @Override
    public void disable() {
        if (eventBus != null && grimPlugin != null) {
            eventBus.unregisterAllListeners(grimPlugin);
            eventBus = null;
        }

        grimPlugin = null;
        enabled = false;
    }

    private void registerListeners() {
        eventBus.subscribe(grimPlugin, GrimTransactionReceivedEvent.class, this::onTransactionReceived);
        eventBus.subscribe(grimPlugin, GrimTeleportEvent.class, this::onTeleport);
    }

    private GrimPlugin createPluginContext() {
        TGPlatform platform = TGPlatform.getInstance();
        return new BasicGrimPlugin(
                platform.getLogger(),
                new File(platform.getPluginDirectory()),
                TGVersions.CURRENT.toString(),
                "TotemGuard",
                List.of("Bram", "OutDev")
        );
    }

    private void onTransactionReceived(GrimTransactionReceivedEvent event) {
        if (!event.isPacketCancelled()) return;
        TGPlayer player = playerRepository.getPlayer(event.getUser().getUniqueId());
        if (player == null) return;

        player.getPingData().transactionReceived(event.getTransactionId(), event.getTimestamp());
        player.getDebugOverlayManager().refresh();
    }

    private void onTeleport(GrimTeleportEvent event) {
        TGPlayer player = playerRepository.getPlayer(event.getUser().getUniqueId());
        if (player == null) return;

        player.getData().getTeleportData().trackTeleport(event.getTeleportId());
        player.getPingData().trackTeleport(event.getTeleportId());
        player.getDebugOverlayManager().refresh();
    }
}
