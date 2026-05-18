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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ApiClassInjector {

    private static final String[] INJECT_PREFIXES = {
            "com/deathmotion/totemguard/api/",
            "com/deathmotion/totemguard/host/"
    };

    private final Logger logger;

    public ApiClassInjector(Logger logger) {
        this.logger = logger;
    }

    private static boolean isAlreadyDefined(ClassLoader target, String className) {
        try {
            Class.forName(className, false, target);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(8192);
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) buf.write(chunk, 0, read);
        return buf.toByteArray();
    }

    public void inject(Path pluginJarPath, ClassLoader target) throws IOException {
        NativeClassLoader.load();

        Map<String, byte[]> pending = new LinkedHashMap<>();
        int alreadyPresent = 0;
        try (JarFile jar = new JarFile(pluginJarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith("META-INF/")) continue;
                if (!entryName.endsWith(".class")) continue;
                boolean matchesPrefix = false;
                for (String prefix : INJECT_PREFIXES) {
                    if (entryName.startsWith(prefix)) {
                        matchesPrefix = true;
                        break;
                    }
                }
                if (!matchesPrefix) continue;

                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                if (pending.containsKey(className)) continue;
                if (isAlreadyDefined(target, className)) {
                    alreadyPresent++;
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    pending.put(className, readAllBytes(in));
                }
            }
        }

        if (pending.isEmpty()) {
            if (alreadyPresent > 0) {
                logger.fine("API classes already injected. Skipping.");
            }
            return;
        }

        long startNanos = System.nanoTime();

        // defineClass resolves the supertype at class-creation time, so a class whose
        // parent has not been defined yet throws ClassNotFoundException. Retry until a
        // pass makes no progress.
        int defined = 0;
        Map<String, Throwable> lastError = new HashMap<>();
        while (!pending.isEmpty()) {
            boolean progress = false;
            Iterator<Map.Entry<String, byte[]>> it = pending.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, byte[]> next = it.next();
                String name = next.getKey();
                byte[] bytes = next.getValue();
                try {
                    NativeClassLoader.defineClass(target, name, bytes);
                    defined++;
                    progress = true;
                    it.remove();
                } catch (Throwable t) {
                    if (isAlreadyDefined(target, name)) {
                        alreadyPresent++;
                        progress = true;
                        it.remove();
                    } else {
                        lastError.put(name, t);
                    }
                }
            }
            if (!progress) break;
        }

        if (!pending.isEmpty()) {
            List<String> failures = new ArrayList<>(pending.size());
            for (String name : pending.keySet()) {
                Throwable t = lastError.get(name);
                Throwable cause = (t != null && t.getCause() != null) ? t.getCause() : t;
                String reason = cause == null ? "unknown"
                        : cause.getClass().getSimpleName()
                          + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
                failures.add(name + " [" + reason + "]");
                if (t != null) logger.log(Level.WARNING, "Failed to inject " + name, t);
            }
            throw new IOException(failures.size() + " API class(es) could not be defined: "
                    + String.join(", ", failures));
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        logger.info("Injected " + defined + " API classes in " + elapsedMs + "ms.");
    }
}
