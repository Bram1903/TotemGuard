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

package com.deathmotion.totemguard.api.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Hosting environment passed by the TotemGuard loader (or a standalone bootstrap)
 * into a {@link TGPluginEntry} when starting the inner plugin.
 */
public interface TGPluginHost {

    /**
     * The runtime platform this host represents.
     */
    @NotNull Platform platform();

    /**
     * The native plugin instance registered with the platform. On Paper this is the
     * loader's {@code JavaPlugin}; on Fabric this is the {@code ModContainer} or an
     * adapter the loader provides. Returned as {@link Object} to keep this interface
     * platform-agnostic; callers cast based on {@link #platform()}.
     */
    @NotNull Object nativePlugin();

    /**
     * The logger the inner plugin should log against. Tagged with {@code TotemGuard}
     * so output is consistent across loader and standalone modes.
     */
    @NotNull Logger logger();

    /**
     * The data directory the inner plugin should use for configs, caches, and
     * versioned jars. Guaranteed to exist before
     * {@link TGPluginEntry#start(TGPluginHost)} is invoked.
     */
    @NotNull Path dataFolder();

    /**
     * The {@link ClassLoader} that owns the host (loader or standalone plugin) and
     * its bundled API surface.
     */
    @NotNull ClassLoader hostClassLoader();

    /**
     * {@code true} when the inner plugin is being driven by the TotemGuard loader.
     * {@code false} when the inner plugin is its own registered plugin (standalone
     * install). The inner uses this to decide whether shutdown should propagate to
     * disabling the registered plugin.
     */
    boolean managedByLoader();

    /**
     * Version of the host driving this inner plugin. {@code null} when the host
     * does not advertise a version (e.g. a standalone plugin acting as its own host).
     */
    @Nullable String hostVersion();
}
