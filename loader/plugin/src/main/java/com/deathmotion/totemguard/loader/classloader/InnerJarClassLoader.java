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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Child classloader for the inner TotemGuard jar. Parent-first delegation routes API
 * class lookups to the loader's plugin classloader (where {@link ApiClassInjector}
 * defined them) so {@code Class<TGUserFlagEvent>} stays stable across hot-reloads.
 */
public final class InnerJarClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    public InnerJarClassLoader(Path jarPath, ClassLoader parent) throws IOException {
        super("totemguard-inner", new URL[]{jarPath.toUri().toURL()}, parent);
    }
}
