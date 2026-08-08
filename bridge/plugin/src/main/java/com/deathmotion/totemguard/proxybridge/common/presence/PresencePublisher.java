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

package com.deathmotion.totemguard.proxybridge.common.presence;

import com.deathmotion.totemguard.proxybridge.common.ProxyIdentity;
import com.deathmotion.totemguard.proxybridge.common.redis.BridgeRedis;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.api.StatefulRedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PresencePublisher {

    private final BridgeRedis redis;
    private final ProxyIdentity identity;
    private final Logger logger;

    public PresencePublisher(@NotNull BridgeRedis redis,
                             @NotNull ProxyIdentity identity,
                             @NotNull Logger logger) {
        this.redis = redis;
        this.identity = identity;
        this.logger = logger;
    }

    public void onJoin(@NotNull UUID playerUuid) {
        publish(BridgeProtocol.EV_PLAYER_JOIN, playerUuid);
    }

    public void onSwitch(@NotNull UUID playerUuid, @org.jetbrains.annotations.Nullable UUID destinationInstance) {
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;
        try {
            String destination = destinationInstance == null ? "" : destinationInstance.toString();
            conn.async().publish(BridgeProtocol.CHANNEL_EVENTS,
                    BridgeProtocol.encode(BridgeProtocol.EV_PLAYER_SWITCH,
                            identity.id().toString(), playerUuid.toString(), destination));
        } catch (Exception ex) {
            logger.log(Level.WARNING, BridgeProtocol.EV_PLAYER_SWITCH + " publish failed: " + ex.getMessage());
        }
    }

    public void onDisconnect(@NotNull UUID playerUuid) {
        publish(BridgeProtocol.EV_PLAYER_DISCONNECT, playerUuid);
    }

    public void onTransfer(@NotNull UUID playerUuid) {
        publish(BridgeProtocol.EV_PLAYER_TRANSFER, playerUuid);
    }

    private void publish(@NotNull String type, @NotNull UUID playerUuid) {
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;
        try {
            conn.async().publish(BridgeProtocol.CHANNEL_EVENTS,
                    BridgeProtocol.encode(type, identity.id().toString(), playerUuid.toString()));
        } catch (Exception ex) {
            logger.log(Level.WARNING, type + " publish failed: " + ex.getMessage());
        }
    }
}
