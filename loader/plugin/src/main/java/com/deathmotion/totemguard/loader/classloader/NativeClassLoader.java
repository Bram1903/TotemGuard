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

package com.deathmotion.totemguard.loader.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NativeClassLoader {

    private static volatile boolean loaded;

    private NativeClassLoader() {
    }

    private static native Class<?> nativeDefineClass(ClassLoader classLoader, String name, byte[] bytes);

    public static synchronized void load() throws IOException {
        if (loaded) return;
        String resourcePath = resolveResourcePath();
        try (InputStream in = NativeClassLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Native classloader bridge is not bundled for the current platform: "
                        + resourcePath + ". Rebuild the loader natives ('./gradlew :loader:compileNative').");
            }
            Path extracted = Files.createTempFile("tgloader_native_", librarySuffix());
            extracted.toFile().deleteOnExit();
            Files.copy(in, extracted, StandardCopyOption.REPLACE_EXISTING);
            System.load(extracted.toAbsolutePath().toString());
        }
        loaded = true;
    }

    public static Class<?> defineClass(ClassLoader target, String name, byte[] bytes) {
        if (!loaded) throw new IllegalStateException("Native classloader bridge is not loaded.");
        return nativeDefineClass(target, name, bytes);
    }

    private static String resolveResourcePath() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String osTag;
        String libPrefix;
        String libExt;
        if (os.contains("linux")) {
            osTag = "linux";
            libPrefix = "lib";
            libExt = ".so";
        } else if (os.contains("windows")) {
            osTag = "windows";
            libPrefix = "";
            libExt = ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osTag = "macos";
            libPrefix = "lib";
            libExt = ".dylib";
        } else {
            throw new IOException("Unsupported OS: " + os);
        }

        String archTag;
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            archTag = "aarch64";
        } else if (arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64")) {
            archTag = "x86_64";
        } else {
            throw new IOException("Unsupported architecture: " + arch);
        }

        return "/natives/" + osTag + "-" + archTag + "/" + libPrefix + "totemguard_loader_native" + libExt;
    }

    private static String librarySuffix() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) return ".dll";
        if (os.contains("mac") || os.contains("darwin")) return ".dylib";
        return ".so";
    }
}
