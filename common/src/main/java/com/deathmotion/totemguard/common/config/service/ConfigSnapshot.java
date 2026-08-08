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
import com.deathmotion.totemguard.common.config.yaml.DefaultsResolver;

import java.util.Map;
import java.util.Objects;

public final class ConfigSnapshot {

    private final ConfigFile file;
    private final Map<String, Object> root;
    private final int version;
    private final DefaultsResolver defaults;
    private final ConfigView view;

    public ConfigSnapshot(ConfigFile file, Map<String, Object> root, int version, DefaultsResolver defaults) {
        this.file = Objects.requireNonNull(file, "file");
        this.root = Objects.requireNonNull(root, "root");
        this.version = version;
        this.defaults = Objects.requireNonNull(defaults, "defaults");
        this.view = new ConfigView(file, version, root, defaults);
    }

    public ConfigFile file() {
        return file;
    }

    public int version() {
        return version;
    }

    public Map<String, Object> root() {
        return root;
    }

    public DefaultsResolver defaults() {
        return defaults;
    }

    public Config view() {
        return view;
    }
}
