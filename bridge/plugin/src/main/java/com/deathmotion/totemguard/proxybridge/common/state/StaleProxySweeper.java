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

package com.deathmotion.totemguard.proxybridge.common.state;

import com.deathmotion.totemguard.proxybridge.common.redis.BridgeRedis;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StaleProxySweeper {

    private final BridgeRedis redis;
    private final Logger logger;

    public StaleProxySweeper(@NotNull BridgeRedis redis, @NotNull Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    public void sweep() {
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;
        try {
            RedisCommands<String, String> cmd = conn.sync();
            Set<String> registered = cmd.smembers(BridgeProtocol.KEY_REGISTRY);
            if (registered == null || registered.isEmpty()) return;
            for (String idStr : registered) {
                if (cmd.exists(BridgeProtocol.KEY_PROXY_PREFIX + idStr) == 0L) {
                    cmd.srem(BridgeProtocol.KEY_REGISTRY, idStr);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Stale proxy sweep failed: " + ex.getMessage());
        }
    }
}
