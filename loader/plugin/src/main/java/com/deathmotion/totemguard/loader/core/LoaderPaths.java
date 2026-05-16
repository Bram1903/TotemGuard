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

package com.deathmotion.totemguard.loader.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public record LoaderPaths(Path loaderDir, Path pluginDataFolder, Path versionsDir, Path localDir) {

    public static LoaderPaths forBukkit(Path bukkitDataFolder) throws IOException {
        // Loader state lives in the loader plugin's own data folder, which Bukkit derives
        // from plugin.yml (TotemGuard-Loader). The TotemGuard plugin's data folder is pinned
        // to plugins/TotemGuard so configs land where standalone installs put them.
        Path loaderDir = bukkitDataFolder.toAbsolutePath();
        Path pluginsDir = loaderDir.getParent();
        Path pluginDataFolder = pluginsDir.resolve("TotemGuard");
        return create(loaderDir, pluginDataFolder);
    }

    public static LoaderPaths forFabric(Path fabricConfigDir) throws IOException {
        Path loaderDir = fabricConfigDir.resolve("totemguard-loader");
        Path pluginDataFolder = fabricConfigDir.resolve("totemguard");
        return create(loaderDir, pluginDataFolder);
    }

    private static LoaderPaths create(Path loaderDir, Path pluginDataFolder) throws IOException {
        Path versionsDir = loaderDir.resolve("versions");
        Path localDir = loaderDir.resolve("local");
        Files.createDirectories(loaderDir);
        Files.createDirectories(versionsDir);
        Files.createDirectories(localDir);
        Files.createDirectories(pluginDataFolder);
        writeLocalReadme(localDir);
        return new LoaderPaths(loaderDir, pluginDataFolder, versionsDir, localDir);
    }

    private static void writeLocalReadme(Path localDir) throws IOException {
        Path readme = localDir.resolve("README.txt");
        if (Files.exists(readme)) return;
        try (InputStream in = LoaderPaths.class.getResourceAsStream("/local-readme.txt")) {
            if (in == null) return;
            Files.copy(in, readme);
        }
    }

    public Path stagedJar() {
        return loaderDir.resolve("staged.jar");
    }

    public Path stagedMeta() {
        return loaderDir.resolve("staged.meta");
    }
}
