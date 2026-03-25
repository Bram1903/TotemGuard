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

package com.deathmotion.totemguard.common.redis.options;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import lombok.Getter;

@Getter
public class RedisOptions {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public RedisOptions() {
        Config config = TGPlatform.getInstance().getConfigRepository().config(ConfigFile.CONFIG);

        enabled = config.getBoolean(ConfigKeys.REDIS_ENABLED);
        host = config.getString(ConfigKeys.REDIS_HOST);
        port = config.getInt(ConfigKeys.REDIS_PORT);
        username = config.getString(ConfigKeys.REDIS_USERNAME);
        password = config.getString(ConfigKeys.REDIS_PASSWORD);
    }
}
