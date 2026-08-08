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

package com.deathmotion.totemguard.loader.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

// Paper-only. The class touches org.yaml.snakeyaml which is bundled by Paper but
// not by Fabric, so it must never be referenced from the Fabric code path.
final class YamlLoaderConfigIO {

    private YamlLoaderConfigIO() {
    }

    static LoaderConfig load(Path loaderDir, Logger logger) throws IOException {
        Files.createDirectories(loaderDir);
        Path configFile = loaderDir.resolve("loader-config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream defaultStream = YamlLoaderConfigIO.class.getResourceAsStream("/loader-config.yml")) {
                if (defaultStream == null) {
                    throw new IOException("Loader default YAML config resource is missing from the jar.");
                }
                Files.copy(defaultStream, configFile);
            }
            logger.info("loader-config.yml: wrote default at " + configFile + ". Edit and restart to change the source.");
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(configFile)) {
            Object parsed = new Yaml().load(in);
            if (!(parsed instanceof Map<?, ?> map)) {
                throw new IOException("loader-config.yml is empty or malformed.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            root = typed;
        }
        return LoaderConfig.fromMap(root, "loader-config.yml", logger);
    }
}
