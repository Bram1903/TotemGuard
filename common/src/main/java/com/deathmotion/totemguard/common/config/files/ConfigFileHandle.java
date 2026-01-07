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

package com.deathmotion.totemguard.common.config.files;

import com.deathmotion.totemguard.common.config.io.NodeIO;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import java.io.IOException;
import java.util.Objects;

public final class ConfigFileHandle {
    private final ConfigFileKey key;
    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private final ConfigurationTransformation.Versioned migrations;
    private final NodeIO nodeIO;

    private volatile CommentedConfigurationNode root;

    public ConfigFileHandle(
            ConfigFileKey key,
            ConfigurationLoader<CommentedConfigurationNode> loader,
            ConfigurationTransformation.Versioned migrations,
            NodeIO nodeIO
    ) {
        this.key = Objects.requireNonNull(key);
        this.loader = Objects.requireNonNull(loader);
        this.migrations = Objects.requireNonNull(migrations);
        this.nodeIO = Objects.requireNonNull(nodeIO);
    }

    public ConfigFileKey key() {
        return key;
    }

    public CommentedConfigurationNode root() {
        return root;
    }

    public void reload() throws IOException {
        CommentedConfigurationNode loaded = nodeIO.load(loader);

        migrations.apply(loaded);
        nodeIO.save(loader, loaded);

        this.root = loaded;
    }

    public void save() throws IOException {
        if (root == null) return;
        nodeIO.save(loader, root);
    }
}

