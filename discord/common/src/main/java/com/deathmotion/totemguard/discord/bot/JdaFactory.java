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

package com.deathmotion.totemguard.discord.bot;

import com.deathmotion.totemguard.discord.config.BotConfig;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class JdaFactory {
    private JdaFactory() {
    }

    static @NotNull ShardManager create(@NotNull BotConfig config,
                                        @Nullable Activity initialActivity,
                                        @NotNull Object @NotNull ... listeners) {
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createLight(config.token())
                .setStatus(OnlineStatus.ONLINE)
                .setEnableShutdownHook(false)
                .addEventListeners(listeners);

        if (initialActivity != null) {
            builder.setActivity(initialActivity);
        }

        return builder.build();
    }

    static @Nullable Activity activity(@NotNull String type, @NotNull String text) {
        if (text.isBlank()) return null;
        return switch (type.toUpperCase()) {
            case "PLAYING" -> Activity.playing(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            default -> Activity.watching(text);
        };
    }
}
