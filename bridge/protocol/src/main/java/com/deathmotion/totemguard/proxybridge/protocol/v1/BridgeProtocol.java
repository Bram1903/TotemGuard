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

package com.deathmotion.totemguard.proxybridge.protocol.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BridgeProtocol {

    public static final String VERSION = "v1";

    public static final char FIELD = '';

    public static final String KEY_REGISTRY = "totemguard:proxy:registry";
    public static final String KEY_PROXY_PREFIX = "totemguard:proxy:";
    public static final String KEY_INSTANCE_PREFIX = "totemguard:instance:";
    public static final String SUFFIX_BACKENDS = ":backends";
    public static final String SUFFIX_INSTANCE_SET = ":instance-set";
    public static final String SUFFIX_INSTANCE_PROXY = ":proxy";
    public static final String SUFFIX_SLOT = ":slot:";

    public static final String CHANNEL_EVENTS = "totemguard:proxy:events";
    public static final String CHANNEL_RPC = "totemguard:proxy:rpc";

    public static final String PLUGIN_CHANNEL_BRIDGE = "totemguard:bridge";

    public static final String EV_PROXY_ONLINE = "proxy_online";
    public static final String EV_PROXY_OFFLINE = "proxy_offline";
    public static final String EV_BACKEND_ADDED = "backend_added";
    public static final String EV_BACKEND_REMOVED = "backend_removed";
    public static final String EV_PLAYER_JOIN = "player_join";
    public static final String EV_PLAYER_SWITCH = "player_switch";
    public static final String EV_PLAYER_DISCONNECT = "player_disconnect";
    public static final String EV_PLAYER_TRANSFER = "player_transfer";
    public static final String EV_PROXY_HELLO = "proxy_hello";
    public static final String EV_BACKEND_BOUND = "backend_bound";
    public static final String EV_BACKEND_UNBOUND = "backend_unbound";

    public static final String RPC_CONNECT = "connect";

    public static final String HASH_DISPLAY_NAME = "display_name";
    public static final String HASH_PLATFORM = "platform";
    public static final String HASH_STARTED_AT = "started_at";
    public static final String HASH_UPDATED_AT = "updated_at";

    private BridgeProtocol() {
    }

    public static @NotNull String encode(@NotNull String type, @NotNull String... fields) {
        StringBuilder sb = new StringBuilder(64).append(VERSION).append(FIELD).append(type);
        for (String f : fields) sb.append(FIELD).append(f == null ? "" : f);
        return sb.toString();
    }

    public static @Nullable String[] decode(@NotNull String message) {
        String[] parts = message.split(String.valueOf(FIELD), -1);
        if (parts.length < 2 || !VERSION.equals(parts[0])) return null;
        String[] payload = new String[parts.length - 1];
        System.arraycopy(parts, 1, payload, 0, payload.length);
        return payload;
    }

    public static @NotNull String keyProxy(@NotNull UUID proxyId) {
        return KEY_PROXY_PREFIX + proxyId;
    }

    public static @NotNull String keyProxyBackends(@NotNull UUID proxyId) {
        return KEY_PROXY_PREFIX + proxyId + SUFFIX_BACKENDS;
    }

    public static @NotNull String keyProxyInstanceSet(@NotNull UUID proxyId) {
        return KEY_PROXY_PREFIX + proxyId + SUFFIX_INSTANCE_SET;
    }

    public static @NotNull String keyInstanceProxy(@NotNull UUID instanceId) {
        return KEY_INSTANCE_PREFIX + instanceId + SUFFIX_INSTANCE_PROXY;
    }

    public static @NotNull String keyProxySlot(@NotNull UUID proxyId, @NotNull String slot) {
        return KEY_PROXY_PREFIX + proxyId + SUFFIX_SLOT + slot;
    }
}
