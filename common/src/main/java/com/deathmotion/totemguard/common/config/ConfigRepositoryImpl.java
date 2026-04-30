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
import com.deathmotion.totemguard.common.config.legacy.V2ConfigMigrator;
import com.deathmotion.totemguard.common.config.migration.MigrationRegistry;
import com.deathmotion.totemguard.common.config.service.ConfigService;
import com.deathmotion.totemguard.common.config.service.ConfigSnapshot;
import com.deathmotion.totemguard.common.config.view.ChecksView;
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.config.view.DiscordView;
import com.deathmotion.totemguard.common.config.view.ModsView;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default {@link ConfigRepository} implementation.
 * <p>
 * Atomic {@link AtomicReference}s per file ensure reads see either the old snapshot or
 * the new one across reloads. Typed views ({@link ChecksView}, {@link ModsView},
 * {@link DiscordView}) are internal types — accessed directly off this concrete impl
 * (which is what {@link TGPlatform#getConfigRepository()} returns) rather than through
 * the public {@link ConfigRepository} interface.
 */
public final class ConfigRepositoryImpl implements ConfigRepository {

    private final Path configDir;
    private final ConfigService service;

    private final EnumMap<ConfigFile, AtomicReference<ConfigSnapshot>> snapshots = new EnumMap<>(ConfigFile.class);

    private final AtomicReference<ConfigView> configView = new AtomicReference<>();
    private final AtomicReference<ChecksView> checksView = new AtomicReference<>();
    private final AtomicReference<ModsView> modsView = new AtomicReference<>();
    private final AtomicReference<DiscordView> discordView = new AtomicReference<>();

    public ConfigRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.configDir = Paths.get(Objects.requireNonNull(platform.getPluginDirectory(), "pluginDirectory"));

        V2ConfigMigrator migrator = new V2ConfigMigrator(platform.getLogger());
        V2ConfigMigrator.V2Migration migration = migrator.migrate(configDir);

        this.service = new ConfigService(
                configDir,
                ConfigRepositoryImpl.class.getClassLoader(),
                MigrationRegistry.buildDefault()
        );

        for (ConfigFile f : ConfigFile.values()) {
            snapshots.put(f, new AtomicReference<>());
        }

        reloadAll();

        if (!migration.isEmpty()) {
            migrator.applyOverrides(configDir, migration);
            reloadAll();
        }
    }

    @Override
    public @NotNull Path configDirectory() {
        return configDir;
    }

    @Override
    public @NotNull Config config(@NotNull ConfigFile file) {
        ConfigSnapshot snap = snapshots.get(file).get();
        if (snap == null) {
            throw new IllegalStateException("Config not loaded: " + file);
        }
        return snap.view();
    }

    public @NotNull ConfigView configView() {
        return configView.get();
    }

    public @NotNull ChecksView checks() {
        return checksView.get();
    }

    public @NotNull ModsView mods() {
        return modsView.get();
    }

    public @NotNull DiscordView discord() {
        return discordView.get();
    }

    @Override
    public void reload(@NotNull ConfigFile file) {
        ConfigSnapshot snap = service.loadAndMigrate(file);
        snapshots.get(file).set(snap);
        rebuildTypedViews(file, snap);
    }

    @Override
    public void reloadAll() {
        for (ConfigFile f : ConfigFile.values()) {
            ConfigSnapshot snap = service.loadAndMigrate(f);
            snapshots.get(f).set(snap);
            rebuildTypedViews(f, snap);
        }
    }

    private void rebuildTypedViews(ConfigFile file, ConfigSnapshot snap) {
        switch (file) {
            case CONFIG -> configView.set(new ConfigView(snap.view()));
            case CHECKS -> checksView.set(new ChecksView(snap.view()));
            case MODS -> modsView.set(new ModsView(snap.view()));
            case DISCORD -> discordView.set(new DiscordView(snap.view()));
            default -> {
            }
        }
    }
}
