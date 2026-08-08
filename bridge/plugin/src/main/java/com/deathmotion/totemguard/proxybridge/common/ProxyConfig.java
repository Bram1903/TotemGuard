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

package com.deathmotion.totemguard.proxybridge.common;

public record ProxyConfig(
        boolean enabled,
        String displayName,
        Redis redis
) {

    public static ProxyConfig defaults() {
        return new ProxyConfig(false, "proxy", Redis.defaults());
    }

    public record Redis(
            String host,
            int port,
            String username,
            String password,
            int database,
            boolean tls
    ) {
        public static Redis defaults() {
            return new Redis("127.0.0.1", 6379, "", "", 0, false);
        }
    }
}
