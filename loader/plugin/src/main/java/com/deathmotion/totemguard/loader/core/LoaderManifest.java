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

import java.io.File;
import java.security.CodeSource;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class LoaderManifest {

    private LoaderManifest() {
    }

    public static String loaderVersion() {
        try {
            CodeSource codeSource = LoaderManifest.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) return "unknown";
            File jarFile = new File(codeSource.getLocation().toURI());
            if (!jarFile.isFile()) return "dev";
            try (JarFile jar = new JarFile(jarFile)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) return "unknown";
                String value = manifest.getMainAttributes().getValue("Implementation-Version");
                return value != null ? value : "unknown";
            }
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
