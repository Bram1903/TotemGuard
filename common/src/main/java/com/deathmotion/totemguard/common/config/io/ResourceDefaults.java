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

package com.deathmotion.totemguard.common.config.io;

import com.deathmotion.totemguard.common.config.files.ConfigFileKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public final class ResourceDefaults {

    public void ensureDefaultsExist(ConfigPaths paths, ClassLoader classLoader) throws IOException {
        Files.createDirectories(paths.pluginDir());

        for (ConfigFileKey key : ConfigFileKey.values()) {
            Path target = paths.filePath(key);
            if (Files.exists(target)) continue;

            try (InputStream in = classLoader.getResourceAsStream(key.fileName())) {
                if (in == null) {
                    throw new IOException("Bundled resource not found: " + key.resourcePath());
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}

