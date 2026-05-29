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

package com.deathmotion.totemguard.discord.fabric;

import com.deathmotion.totemguard.discord.DiscordPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

final class FabricDiscordPlatform implements DiscordPlatform {
    private static final String LOGGER_NAME = "TotemGuard-Discord";

    private final Logger logger;
    private final Path dataDirectory;
    private volatile MinecraftServer server;

    FabricDiscordPlatform() {
        this.logger = bridgedLogger();
        this.dataDirectory = FabricLoader.getInstance().getConfigDir().resolve("totemguard-discord");
    }

    private static Logger bridgedLogger() {
        Logger jul = Logger.getLogger(LOGGER_NAME);
        org.slf4j.Logger slf4j = org.slf4j.LoggerFactory.getLogger(LOGGER_NAME);
        jul.setUseParentHandlers(false);
        for (Handler handler : jul.getHandlers()) jul.removeHandler(handler);
        jul.addHandler(new Slf4jBridgeHandler(slf4j));
        jul.setLevel(Level.ALL);
        return jul;
    }

    void setServer(@Nullable MinecraftServer server) {
        this.server = server;
    }

    @Override
    public @NotNull Logger logger() {
        return logger;
    }

    @Override
    public @NotNull Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public @NotNull Optional<UUID> resolveUuid(@NotNull String name) {
        MinecraftServer current = this.server;
        if (current == null) return Optional.empty();
        ServerPlayer online = current.getPlayerList().getPlayerByName(name);
        return online == null ? Optional.empty() : Optional.of(online.getUUID());
    }

    private static final class Slf4jBridgeHandler extends Handler {
        private final org.slf4j.Logger target;

        Slf4jBridgeHandler(org.slf4j.Logger target) {
            this.target = target;
        }

        @Override
        public void publish(LogRecord record) {
            if (record == null) return;
            String message = record.getMessage();
            Object[] params = record.getParameters();
            if (message != null && params != null && params.length > 0) {
                try {
                    message = MessageFormat.format(message, params);
                } catch (IllegalArgumentException ignored) {
                }
            }
            Throwable thrown = record.getThrown();
            int level = record.getLevel().intValue();
            if (level >= Level.SEVERE.intValue()) target.error(message, thrown);
            else if (level >= Level.WARNING.intValue()) target.warn(message, thrown);
            else if (level >= Level.INFO.intValue()) target.info(message, thrown);
            else target.debug(message, thrown);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
