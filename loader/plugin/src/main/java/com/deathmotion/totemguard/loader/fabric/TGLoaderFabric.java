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

package com.deathmotion.totemguard.loader.fabric;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.loader.core.*;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class TGLoaderFabric implements DedicatedServerModInitializer {

    private final Logger logger = Logger.getLogger("TotemGuardLoader");
    private volatile PluginRuntime runtime;
    private volatile LoaderCore core;

    @Override
    public void onInitializeServer() {
        if (!new JarIntegrityChecker(logger, "TotemGuard-Loader").verifyCurrentJar()) {
            logger.severe("TotemGuard Loader will NOT load the TotemGuard plugin because the loader jar failed integrity verification.");
            return;
        }
        logger.info("TotemGuard Loader " + LoaderManifest.loaderVersion() + " on Fabric");

        try {
            LoaderPaths paths = LoaderPaths.forFabric(FabricLoader.getInstance().getConfigDir());
            this.core = new LoaderCore(logger, paths, HostPlatform.FABRIC);
            LoaderResult result = core.run(getClass().getClassLoader());

            this.runtime = new PluginRuntime(core, this, paths, getClass().getClassLoader(), logger);
            this.runtime.start(result.pluginJar());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (runtime != null) {
                    try {
                        runtime.shutdown();
                    } catch (Throwable ignored) {
                    }
                }
                if (core != null) {
                    try {
                        core.shutdown();
                    } catch (Throwable ignored) {
                    }
                }
            }, "TotemGuard-Loader-Shutdown"));
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "TotemGuard Loader failed to start the TotemGuard plugin", t);
        }
    }
}
