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

package com.deathmotion.totemguard.common.network.bridge;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BridgeHandshakeListener {

    private static final int MAX_PAYLOAD_BYTES = 256;
    private static final int MAX_SLOT_NAME_LENGTH = 64;
    private static final long PROXY_HEARTBEAT_TOLERANCE_MILLIS = 120_000L;

    private static final byte[] EXPECTED_PREFIX =
            (BridgeProtocol.VERSION + BridgeProtocol.FIELD
                    + BridgeProtocol.EV_PROXY_HELLO + BridgeProtocol.FIELD)
                    .getBytes(StandardCharsets.UTF_8);
    private static final int MIN_PAYLOAD_BYTES = EXPECTED_PREFIX.length + 36 + 1 + 1 + 1;

    private final TGPlatform platform;
    private final Logger logger;
    private final RedisRepositoryImpl redis;
    private final BridgeBindingHeartbeat heartbeat;
    private final Set<UUID> processedPlayers = ConcurrentHashMap.newKeySet();

    public BridgeHandshakeListener(@NotNull TGPlatform platform,
                                   @NotNull BridgeBindingHeartbeat heartbeat) {
        this.platform = platform;
        this.logger = platform.getLogger();
        this.redis = platform.getRedisRepository();
        this.heartbeat = heartbeat;
    }

    private static boolean hasExpectedPrefix(byte[] data) {
        for (int i = 0; i < EXPECTED_PREFIX.length; i++) {
            if (data[i] != EXPECTED_PREFIX[i]) return false;
        }
        return true;
    }

    private static boolean isValidSlotName(String name) {
        if (name.isEmpty() || name.length() > MAX_SLOT_NAME_LENGTH) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x21 || c > 0x7E) return false;
        }
        return true;
    }

    public void handle(@NotNull UUID playerUuid, @NotNull String playerName, byte @NotNull [] data) {
        if (!processedPlayers.add(playerUuid)) return;
        if (data.length < MIN_PAYLOAD_BYTES || data.length > MAX_PAYLOAD_BYTES) return;
        if (!hasExpectedPrefix(data)) return;
        if (!redis.isClusterMode() || !redis.isConnected()) return;

        String payload;
        try {
            payload = new String(data, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return;
        }
        String[] parts = BridgeProtocol.decode(payload);
        if (parts == null || parts.length < 4) return;
        if (!BridgeProtocol.EV_PROXY_HELLO.equals(parts[0])) return;

        UUID proxyId;
        try {
            proxyId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        String slot = parts[2];
        String displayName = parts[3];
        if (!isValidSlotName(slot)) return;

        verifyAndBind(playerName, proxyId, slot, displayName);
    }

    public void forget(@NotNull UUID playerUuid) {
        processedPlayers.remove(playerUuid);
    }

    private void verifyAndBind(String playerName, UUID proxyId, String slot, String displayName) {
        RedisConnection conn = redis.connection();
        if (conn == null || !conn.isOpen()) return;
        byte[] proxyKey = BridgeProtocol.keyProxy(proxyId).getBytes(StandardCharsets.UTF_8);
        byte[] updatedAtField = BridgeProtocol.HASH_UPDATED_AT.getBytes(StandardCharsets.UTF_8);
        byte[] displayNameField = BridgeProtocol.HASH_DISPLAY_NAME.getBytes(StandardCharsets.UTF_8);

        conn.commands().async().hmget(proxyKey, updatedAtField, displayNameField)
                .whenComplete((kvs, error) -> {
                    if (error != null) {
                        logger.log(Level.FINE,
                                "Bridge handshake Redis lookup failed: " + error.getMessage());
                        return;
                    }
                    long updatedAt = 0L;
                    String storedDisplayName = null;
                    if (kvs != null) {
                        for (var kv : kvs) {
                            if (!kv.hasValue()) continue;
                            byte[] field = kv.getKey();
                            byte[] value = kv.getValue();
                            if (Arrays.equals(field, updatedAtField)) {
                                try {
                                    updatedAt = Long.parseLong(new String(value, StandardCharsets.UTF_8));
                                } catch (NumberFormatException ignored) {
                                }
                            } else if (Arrays.equals(field, displayNameField)) {
                                storedDisplayName = new String(value, StandardCharsets.UTF_8);
                            }
                        }
                    }
                    long age = System.currentTimeMillis() - updatedAt;
                    boolean stale = updatedAt == 0L || age > PROXY_HEARTBEAT_TOLERANCE_MILLIS;
                    boolean mismatch = storedDisplayName != null && !storedDisplayName.equals(displayName);
                    if (stale || mismatch) {
                        logger.log(Level.FINE, "Bridge handshake dropped for " + playerName
                                + " (proxy " + proxyId + " " + (stale ? "stale" : "display name mismatch") + ")");
                        return;
                    }
                    heartbeat.acceptHandshake(proxyId, slot, displayName);
                });
    }
}
