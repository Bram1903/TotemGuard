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
import java.nio.file.Files;
import java.nio.file.Path;

public record LoaderPaths(Path loaderDir, Path innerDataFolder, Path versionsDir, Path localDir) {

    public static LoaderPaths forBukkit(Path bukkitDataFolder) throws IOException {
        Path pluginsDir = bukkitDataFolder.toAbsolutePath().getParent();
        Path loaderDir = pluginsDir.resolve("TotemGuardLoader");
        Path inner = bukkitDataFolder.toAbsolutePath();
        return create(loaderDir, inner);
    }

    public static LoaderPaths forFabric(Path fabricConfigDir) throws IOException {
        Path loaderDir = fabricConfigDir.resolve("totemguardloader");
        Path inner = fabricConfigDir.resolve("totemguard");
        return create(loaderDir, inner);
    }

    private static LoaderPaths create(Path loaderDir, Path innerDataFolder) throws IOException {
        Path versionsDir = loaderDir.resolve("versions");
        Path localDir = loaderDir.resolve("local");
        Files.createDirectories(loaderDir);
        Files.createDirectories(versionsDir);
        Files.createDirectories(innerDataFolder);
        return new LoaderPaths(loaderDir, innerDataFolder, versionsDir, localDir);
    }

    public Path stagedJar() {
        return loaderDir.resolve("staged.jar");
    }

    public Path stagedMeta() {
        return loaderDir.resolve("staged.meta");
    }
}
