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

package com.deathmotion.totemguard.common.redis.cache;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Centralized Redis key definitions for TotemGuard.
 *
 * <p>All Redis keys should be created through this class to ensure consistent
 * namespacing and to avoid accidental collisions.</p>
 */
public final class RedisKeys {

    private static final String PREFIX = "totemguard";

    private RedisKeys() {
    }

    public static byte[] vpnData(String ip) {
        return bytes(PREFIX + ":vpn:" + ip);
    }

    /**
     * Key for cached check snapshots.
     */
    public static byte[] checkSnapshots(UUID uuid) {
        return bytes(PREFIX + ":checks:" + uuid);
    }

    public static byte[] punishQueue(UUID uuid) {
        return bytes(PREFIX + ":punishQueue:" + uuid);
    }

    public static byte[] alertsToggleData(UUID uuid) {
        return bytes(PREFIX + ":alertsToggleData:" + uuid);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}