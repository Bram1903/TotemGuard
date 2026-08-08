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

package com.deathmotion.totemguard.common.config.schema;

import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import org.jetbrains.annotations.NotNull;

public record RedisOptions(
        boolean enabled,
        boolean cluster,
        @NotNull String host,
        int port,
        @NotNull String username,
        @NotNull String password,
        @NotNull MessagingOptions messaging
) {

    public record MessagingOptions(@NotNull AlertsOptions alerts) {

        public @NotNull String channelFor(@NotNull MessagingTopic topic) {
            String override = topic == MessagingTopic.ALERTS ? alerts.channel() : "";
            return topic.channelName(override);
        }
    }

    public record AlertsOptions(@NotNull String channel, boolean send, boolean receive) {
    }
}
