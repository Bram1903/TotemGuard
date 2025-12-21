/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.config;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.codec.MessageFormat;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ConfigRepositoryImpl {

    private static final DateTimeFormatter BROKEN_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ConfigurationNode config;
    private ConfigurationNode messages;
    private ConfigurationNode checks;

    public boolean reload() {
        try {
            final ConfigurationNode config =
                    loadOrDisable("config.yml", ConfigLoaderFactory.yaml("config.yml"));
            if (config == null) {
                return false;
            }

            MessageFormat format = MessageFormat.NATIVE;
            try {
                format = config.node("formatter").get(MessageFormat.class, MessageFormat.NATIVE);
            } catch (final SerializationException e) {
                TGPlatform.getInstance().getLogger().severe("Invalid formatter in config.yml defaulting to NATIVE.");
            }

            final ConfigurationNode messages =
                    loadOrDisable(
                            "messages.yml",
                            ConfigLoaderFactory.messagesYaml("messages.yml", format)
                    );
            if (messages == null) {
                return false;
            }

            final ConfigurationNode checks =
                    loadOrDisable("checks.yml", ConfigLoaderFactory.yaml("checks.yml"));
            if (checks == null) {
                return false;
            }

            // Commit only after everything succeeded
            this.config = config;
            this.messages = messages;
            this.checks = checks;

            return true;

        } catch (final Throwable t) {
            disable("Unexpected error while reloading configuration.", t);
            return false;
        }
    }

    public ConfigurationNode config() {
        ensureLoaded(config, "config.yml");
        return config;
    }

    public ConfigurationNode messages() {
        ensureLoaded(messages, "messages.yml");
        return messages;
    }

    public ConfigurationNode checks() {
        ensureLoaded(checks, "checks.yml");
        return checks;
    }

    private static void ensureLoaded(final ConfigurationNode node, final String name) {
        if (node == null) {
            throw new IllegalStateException("Configuration not loaded yet: " + name);
        }
    }

    /**
     * Loads a configuration file, attempting recovery (move broken file + restore defaults)
     * on Configurate parsing errors.
     *
     * @return loaded node, or null if the plugin was disabled
     */
    private static ConfigurationNode loadOrDisable(
            final String fileName,
            final YamlConfigurationLoader loader
    ) {
        final Path target = ConfigLoaderFactory.resolve(fileName);

        try {
            if (Files.notExists(target)) {
                restoreDefault(fileName, target);
            }

            return loader.load();

        } catch (final ConfigurateException firstFailure) {
            try {
                if (Files.exists(target)) {
                    final Path broken = brokenName(target);
                    Files.move(target, broken, StandardCopyOption.REPLACE_EXISTING);

                    TGPlatform.getInstance().getLogger().warning(
                            "Configuration " + fileName +
                                    " is broken, moved to " + broken.getFileName() +
                                    " and restoring defaults."
                    );
                }

                restoreDefault(fileName, target);
                return loader.load();

            } catch (final Throwable t) {
                t.addSuppressed(firstFailure);
                disable("Failed to restore/load default configuration for " + fileName + ".", t);
                return null;
            }

        } catch (final Throwable t) {
            disable("Unexpected error while loading " + fileName + ".", t);
            return null;
        }
    }

    private static void restoreDefault(final String fileName, final Path target)
            throws IOException {
        Files.createDirectories(target.getParent());

        try (var in = TGPlatform.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (in == null) {
                throw new IOException("Default resource not found: " + fileName);
            }

            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path brokenName(final Path original) {
        final String name = original.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        final String base = dot > 0 ? name.substring(0, dot) : name;
        final String ext = dot > 0 ? name.substring(dot) : "";
        return original.resolveSibling(
                base + "_broken_" + LocalDateTime.now().format(BROKEN_TS) + ext
        );
    }

    private static void disable(final String message, final Throwable cause) {
        final TGPlatform platform = TGPlatform.getInstance();
        platform.getLogger().severe(message);
        platform.getLogger().severe(String.valueOf(cause.getMessage()));
        platform.disablePlugin();
    }
}
