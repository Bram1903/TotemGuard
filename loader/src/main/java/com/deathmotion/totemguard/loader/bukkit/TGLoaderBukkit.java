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

package com.deathmotion.totemguard.loader.bukkit;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.loader.core.*;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class TGLoaderBukkit extends JavaPlugin {

    private final Logger loaderLog = Logger.getLogger("TotemGuardLoader");

    private volatile boolean integrityFailed;
    private volatile PluginRuntime runtime;

    @Override
    public void onLoad() {
        if (!new JarIntegrityChecker(loaderLog, "TotemGuard-Loader").verifyCurrentJar()) {
            integrityFailed = true;
        }
    }

    @Override
    public void onEnable() {
        if (integrityFailed) {
            loaderLog.severe("TotemGuard Loader will NOT load the inner plugin because the loader jar failed integrity verification.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        loaderLog.info("TotemGuard Loader " + LoaderManifest.loaderVersion() + " on Paper");

        try {
            LoaderPaths paths = LoaderPaths.forBukkit(getDataFolder().toPath());
            LoaderCore core = new LoaderCore(loaderLog, paths, HostPlatform.PAPER);
            LoaderResult result = core.run(getClassLoader());

            this.runtime = new PluginRuntime(core, this, paths, getClassLoader(), loaderLog);
            this.runtime.start(result.innerJar());

            registerCommand();
        } catch (Throwable t) {
            loaderLog.log(Level.SEVERE, "TotemGuard Loader failed to start the inner plugin", t);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (runtime == null) return;
        try {
            runtime.shutdown();
        } catch (Throwable t) {
            loaderLog.log(Level.WARNING, "TotemGuard Loader shutdown threw", t);
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("tgloader");
        if (command == null) return;
        BukkitTgLoaderCommand executor = new BukkitTgLoaderCommand(this, runtime);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
