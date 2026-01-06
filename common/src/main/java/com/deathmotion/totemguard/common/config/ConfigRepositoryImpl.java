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

package com.deathmotion.totemguard.common.config;

import com.deathmotion.totemguard.common.TGPlatform;
import lombok.Getter;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public final class ConfigRepositoryImpl {

    private static final DateTimeFormatter BROKEN_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private CommentedConfigurationNode config;
    private CommentedConfigurationNode messages;
    private CommentedConfigurationNode checks;

    private static CommentedConfigurationNode loadYamlWithFallback(final String fileName) {
        final Path target = ConfigLoaderFactory.resolve(fileName);
        final YamlConfigurationLoader loader = ConfigLoaderFactory.yaml(fileName);

        ensurePresentFromResource(fileName, target);

        try {
            return loader.load();
        } catch (final ConfigurateException firstFailure) {
            try {
                moveToBrokenNameIfExists(target);
                TGPlatform.getInstance().getLogger().warning(
                        "Configuration " + fileName + " is invalid; restoring defaults."
                );

                ensurePresentFromResource(fileName, target);
                return loader.load();
            } catch (final Throwable t) {
                t.addSuppressed(firstFailure);
                disable("Failed to restore/load default configuration for " + fileName + ".", t);
                return CommentedConfigurationNode.root();
            }
        } catch (final Throwable t) {
            disable("Unexpected error while loading " + fileName + ".", t);
            return CommentedConfigurationNode.root();
        }
    }

    private static void ensurePresentFromResource(final String fileName, final Path target) {
        try {
            Files.createDirectories(target.getParent());

            if (Files.exists(target)) {
                return;
            }

            try (InputStream in = TGPlatform.class.getClassLoader().getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new IOException("Default resource not found on classpath: " + fileName);
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException e) {
            disable("Failed to create default configuration file: " + fileName + ".", e);
        }
    }

    private static void moveToBrokenNameIfExists(final Path target) throws IOException {
        if (!Files.exists(target)) {
            return;
        }
        final Path broken = brokenName(target);
        Files.move(target, broken, StandardCopyOption.REPLACE_EXISTING);
        TGPlatform.getInstance().getLogger().warning(
                "Moved broken configuration to " + broken.getFileName()
        );
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

    public boolean reload() {
        try {
            this.config = loadYamlWithFallback("config.yml");
            this.messages = loadYamlWithFallback("messages.yml");
            this.checks = loadYamlWithFallback("checks.yml");
            return true;
        } catch (final Throwable t) {
            disable("Unexpected error while loading configuration.", t);
            return false;
        }
    }

    public String messagePrefix() {
        return this.messages.node("prefix").getString("&6&lTG &8»");
    }

    public String message(final String key, final String def) {
        return this.messages.node(key).getString(def);
    }
}
