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

/**
 * Service-loader entry point exposed by an inner TotemGuard plugin jar.
 * <p>
 * The TotemGuard loader discovers implementations via
 * {@link java.util.ServiceLoader#load(Class, ClassLoader)} and picks the one whose
 * {@link #platform()} matches the running host. Implementations are registered
 * through {@code META-INF/services/com.deathmotion.totemguard.api.host.TGPluginEntry}.
 */
public interface TGPluginEntry {

    /**
     * The platform this entry can start on.
     */
    @NotNull Platform platform();

    /**
     * Starts the inner plugin against the given host. Runs the entire enable
     * lifecycle synchronously on the calling thread and returns only once the
     * plugin is fully initialized or has irrecoverably failed.
     *
     * @throws Exception if startup fails; the loader logs and leaves itself running
     */
    @NotNull TGPluginHandle start(@NotNull TGPluginHost host) throws Exception;
}
