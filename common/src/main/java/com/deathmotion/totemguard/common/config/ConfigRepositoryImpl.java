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

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.api.config.ConfigRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.impl.mods.ModSignatures;
import com.deathmotion.totemguard.common.config.migration.MigrationRegistry;
import com.deathmotion.totemguard.common.config.service.ConfigService;
import com.deathmotion.totemguard.common.config.service.ConfigSnapshot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfigRepositoryImpl implements ConfigRepository {

    private final Path configDir;
    private final ConfigService service;

    private final EnumMap<ConfigFile, AtomicReference<ConfigSnapshot>> snapshots =
            new EnumMap<>(ConfigFile.class);

    public ConfigRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.configDir = Paths.get(Objects.requireNonNull(platform.getPluginDirectory(), "pluginDirectory"));

        this.service = new ConfigService(
                configDir,
                ConfigRepositoryImpl.class.getClassLoader(),
                new MigrationRegistry().buildDefault()
        );

        for (ConfigFile f : ConfigFile.values()) {
            snapshots.put(f, new AtomicReference<>());
        }

        reloadAll();
    }

    @Override
    public Path configDirectory() {
        return configDir;
    }

    @Override
    public Config config(ConfigFile file) {
        ConfigSnapshot snap = snapshots.get(file).get();
        if (snap == null) {
            throw new IllegalStateException("Config not loaded: " + file);
        }
        return snap.view();
    }

    @Override
    public void reload(ConfigFile file) {
        ConfigSnapshot newSnap = service.loadAndMigrate(file);
        snapshots.get(file).set(newSnap);

        if (file == ConfigFile.MODS) {
            ModSignatures.load(newSnap.view());
        }
    }

    @Override
    public void reloadAll() {
        for (ConfigFile f : ConfigFile.values()) {
            reload(f);
        }
    }
}
