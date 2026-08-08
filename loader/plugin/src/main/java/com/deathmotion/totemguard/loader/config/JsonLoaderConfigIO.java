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

import com.google.gson.Gson;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

// Fabric-only. Uses gson (bundled by fabric-loader), reads loader-config.json with
// lenient parsing so the shipped default can carry // comments.
final class JsonLoaderConfigIO {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private JsonLoaderConfigIO() {
    }

    static LoaderConfig load(Path loaderDir, Logger logger) throws IOException {
        Files.createDirectories(loaderDir);
        Path configFile = loaderDir.resolve("loader-config.json");
        if (!Files.exists(configFile)) {
            try (InputStream defaultStream = JsonLoaderConfigIO.class.getResourceAsStream("/loader-config.json")) {
                if (defaultStream == null) {
                    throw new IOException("Loader default JSON config resource is missing from the jar.");
                }
                Files.copy(defaultStream, configFile);
            }
            logger.info("loader-config.json: wrote default at " + configFile + ". Edit and restart to change the source.");
        }

        String text = Files.readString(configFile, StandardCharsets.UTF_8);
        Map<String, Object> root;
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            // Lenient mode permits // and /* */ comments in the shipped default.
            reader.setStrictness(Strictness.LENIENT);
            root = new Gson().fromJson(reader, MAP_TYPE);
        }
        if (root == null) {
            throw new IOException("loader-config.json is empty or malformed.");
        }
        return LoaderConfig.fromMap(root, "loader-config.json", logger);
    }
}
