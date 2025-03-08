/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.redis.handlers;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.interfaces.Reloadable;
import com.deathmotion.totemguard.redis.packet.PacketProcessor;
import com.deathmotion.totemguard.redis.packet.PacketRegistry;
import com.deathmotion.totemguard.redis.packet.Packets;
import net.kyori.adventure.text.Component;

/**
 * Handler for processing synchronization of alert messages.
 */
public class SyncAlertMessageHandler implements PacketProcessor<Component>, Reloadable {

    private final TotemGuard plugin;
    private final PacketRegistry registry;

    public SyncAlertMessageHandler(TotemGuard plugin, PacketRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;

        registry.registerProcessor(Packets.SYNC_ALERT_MESSAGE.getId(), this);
    }

    @Override
    public void handle(Component component) {
        plugin.getAlertManager().sendAlert(component);
    }

    @Override
    public void reload() {
        registry.unregister(Packets.SYNC_ALERT_MESSAGE.getId(), this);
        if (!plugin.getConfigManager().getSettings().getRedis().isSyncAlerts()) return;
        registry.registerProcessor(Packets.SYNC_ALERT_MESSAGE.getId(), this);
    }
}

