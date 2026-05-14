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

package com.deathmotion.totemguard.loader.runtime;

import com.deathmotion.totemguard.api.host.Platform;
import com.deathmotion.totemguard.api.host.TGPluginHost;
import com.deathmotion.totemguard.loader.core.LoaderManifest;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.logging.Logger;

final class PluginHost implements TGPluginHost {

    private final Platform platform;
    private final Object nativePlugin;
    private final Logger logger;
    private final Path dataFolder;
    private final ClassLoader hostClassLoader;

    PluginHost(Platform platform, Object nativePlugin, Logger logger, Path dataFolder,
               ClassLoader hostClassLoader) {
        this.platform = platform;
        this.nativePlugin = nativePlugin;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.hostClassLoader = hostClassLoader;
    }

    @Override
    public @NotNull Platform platform() {
        return platform;
    }

    @Override
    public @NotNull Object nativePlugin() {
        return nativePlugin;
    }

    @Override
    public @NotNull Logger logger() {
        return logger;
    }

    @Override
    public @NotNull Path dataFolder() {
        return dataFolder;
    }

    @Override
    public @NotNull ClassLoader hostClassLoader() {
        return hostClassLoader;
    }

    @Override
    public boolean managedByLoader() {
        return true;
    }

    @Override
    public String hostVersion() {
        return LoaderManifest.loaderVersion();
    }

    @Override
    public String toString() {
        return "TGPluginHost(loader=" + LoaderManifest.loaderVersion() + ")";
    }
}
