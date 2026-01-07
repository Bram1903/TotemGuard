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
import com.deathmotion.totemguard.common.config.files.ConfigFileHandle;
import com.deathmotion.totemguard.common.config.files.ConfigFileKey;
import com.deathmotion.totemguard.common.config.io.ConfigPaths;
import com.deathmotion.totemguard.common.config.io.NodeIO;
import com.deathmotion.totemguard.common.config.io.ResourceDefaults;
import com.deathmotion.totemguard.common.config.io.YamlLoaderFactory;
import com.deathmotion.totemguard.common.config.migrate.MigrationRegistry;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class ConfigRepositoryImpl {

    private final ConfigPaths paths;
    private final ResourceDefaults defaults = new ResourceDefaults();
    private final YamlLoaderFactory loaderFactory = new YamlLoaderFactory();
    private final NodeIO nodeIO = new NodeIO();
    private final MigrationRegistry migrations = new MigrationRegistry();

    private final Map<ConfigFileKey, ConfigFileHandle> files = new EnumMap<>(ConfigFileKey.class);

    public ConfigRepositoryImpl() {
        this.paths = new ConfigPaths(TGPlatform.getInstance().getPluginDirectory());
        initHandles();
    }

    private void initHandles() {
        for (ConfigFileKey key : ConfigFileKey.values()) {
            Path path = paths.filePath(key);
            ConfigurationLoader<CommentedConfigurationNode> loader = loaderFactory.create(path);

            files.put(key, new ConfigFileHandle(
                    key,
                    loader,
                    migrations.forFile(key),
                    nodeIO
            ));
        }
    }

    public void reload() {
        try {
            defaults.ensureDefaultsExist(paths, getClass().getClassLoader());

            for (ConfigFileHandle handle : files.values()) {
                handle.reload();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load configuration files", e);
        }
    }

    public void save() {
        try {
            for (ConfigFileHandle handle : files.values()) {
                handle.save();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save configuration files", e);
        }
    }

    public CommentedConfigurationNode config() {
        return node(ConfigFileKey.MAIN);
    }

    public CommentedConfigurationNode checks() {
        return node(ConfigFileKey.CHECKS);
    }

    public CommentedConfigurationNode messages() {
        return node(ConfigFileKey.MESSAGES);
    }

    public Path dataDirectory() {
        return paths.pluginDir();
    }

    private CommentedConfigurationNode node(ConfigFileKey key) {
        ConfigFileHandle handle = files.get(key);
        if (handle == null || handle.root() == null) {
            throw new IllegalStateException("Config not loaded: " + key + " (did you call reload()?)");
        }
        return handle.root();
    }
}
