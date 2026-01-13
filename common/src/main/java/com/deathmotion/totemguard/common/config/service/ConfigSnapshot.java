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

package com.deathmotion.totemguard.common.config.service;

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.common.config.path.PathRegistry;
import com.deathmotion.totemguard.common.config.path.PathResolver;
import com.deathmotion.totemguard.common.config.yaml.YamlNavigator;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class ConfigSnapshot {

    private final ConfigFile file;
    private final Path path;
    private final Map<String, Object> root;
    private final int version;

    private final PathResolver resolver;
    private final YamlNavigator navigator;
    private final ConfigView view;

    public ConfigSnapshot(ConfigFile file, Path path, Map<String, Object> root, int version, PathRegistry registry) {
        this.file = Objects.requireNonNull(file, "file");
        this.path = Objects.requireNonNull(path, "path");
        this.root = Objects.requireNonNull(root, "root");
        this.version = version;

        this.resolver = new PathResolver(registry);
        this.navigator = new YamlNavigator(root, registry);
        this.view = new ConfigView(file, version, root, navigator, resolver);
    }

    public Config view() {
        return view;
    }
}
