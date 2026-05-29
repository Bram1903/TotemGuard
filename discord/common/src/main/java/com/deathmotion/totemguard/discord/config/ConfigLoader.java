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

package com.deathmotion.totemguard.discord.config;

import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private static final String FILE_NAME = "config.yml";
    private static final String BUNDLED_DEFAULT = "/discord-config.yml";

    private ConfigLoader() {
    }

    public static @NotNull BotConfig load(@NotNull Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve(FILE_NAME);
        if (Files.notExists(file)) {
            try (InputStream in = ConfigLoader.class.getResourceAsStream(BUNDLED_DEFAULT)) {
                if (in == null) {
                    throw new IOException("Bundled default " + BUNDLED_DEFAULT + " is missing from the jar.");
                }
                Files.copy(in, file);
            }
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object root = new Yaml().load(reader);
            return BotConfig.fromMap(root);
        }
    }
}
