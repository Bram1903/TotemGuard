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
import com.deathmotion.totemguard.loader.fleet.RolloutCoordinator;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class TGLoaderBukkit extends JavaPlugin {

    private final Logger loaderLog = Logger.getLogger("TotemGuard-Loader");

    private volatile boolean integrityFailed;
    private volatile LoaderPaths paths;
    private volatile LoaderCore core;
    private volatile PluginRuntime runtime;

    private static String describeRootCause(Throwable t) {
        Throwable root = t;
        for (int hops = 0; hops < 32; hops++) {
            Throwable next = root.getCause();
            if (next == null || next == root) break;
            root = next;
        }
        String msg = root.getMessage();
        return root.getClass().getSimpleName() + (msg == null || msg.isBlank() ? "" : ": " + msg);
    }

    @Override
    public void onLoad() {
        if (!new JarIntegrityChecker(loaderLog, "TotemGuard-Loader").verifyCurrentJar()) {
            integrityFailed = true;
        }
    }

    @Override
    public void onEnable() {
        // Self-integrity is the only condition that disables the loader. Everything else
        // (network failures, bad pins, missing assets, plugin start crashes) must keep
        // the loader online so operators can recover via /tgloader.
        if (integrityFailed) {
            loaderLog.severe("TotemGuard Loader will NOT load the TotemGuard plugin because the loader jar failed integrity verification.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        loaderLog.info("TotemGuard Loader " + LoaderManifest.loaderVersion() + " on Paper");

        try {
            this.paths = LoaderPaths.forBukkit(getDataFolder().toPath());
            this.core = new LoaderCore(loaderLog, paths, HostPlatform.PAPER);
            this.core.initRolloutCoordinator(this::onFleetApply);
        } catch (Throwable t) {
            loaderLog.log(Level.SEVERE, "TotemGuard Loader could not prepare its data directory. "
                    + "The loader will stay enabled but inert. Check filesystem permissions.", t);
            return;
        }

        registerCommand();

        try {
            attemptStart(null);
        } catch (Throwable t) {
            loaderLog.log(Level.SEVERE, "TotemGuard could not be loaded automatically ("
                    + describeRootCause(t) + "). The loader is still enabled. "
                    + "Recover with /tgloader load <version>, or drop a jar into "
                    + paths.localDir() + " then run /tgloader import followed by "
                    + "/tgloader load <version>.", t);
        }
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            try {
                runtime.shutdown();
            } catch (Throwable t) {
                loaderLog.log(Level.WARNING, "TotemGuard Loader shutdown threw", t);
            }
        }
        if (core != null) {
            try {
                core.shutdown();
            } catch (Throwable t) {
                loaderLog.log(Level.WARNING, "Loader core shutdown threw", t);
            }
        }
    }

    public synchronized void attemptStart(String versionOverride) throws Exception {
        if (runtime == null) {
            LoaderResult result = core.run(getClassLoader(), versionOverride);
            PluginRuntime created = new PluginRuntime(core, this, paths, getClassLoader(), loaderLog);
            created.start(result.pluginJar());
            this.runtime = created;
        } else {
            runtime.loadVersionForCommand(versionOverride);
        }
    }

    public LoaderCore.StageResult attemptStage(String versionOverride) throws Exception {
        return core.stageVersion(versionOverride);
    }

    public synchronized void attemptApplyStaged() throws Exception {
        if (core.readStaged().isEmpty()) {
            throw new IllegalStateException("Nothing is staged. Use /tgloader stage <version> first.");
        }
        if (runtime == null) {
            attemptStart(null);
        } else {
            runtime.loadVersionForCommand(null);
        }
    }

    private void onFleetApply(RolloutCoordinator.RolloutApply apply) {
        if (core.readStaged().isEmpty()) {
            boolean staged = core.stageFromCatalogBySha(apply.targetVersion(), apply.targetSha256());
            if (!staged) {
                loaderLog.warning("Received fleet APPLY for " + apply.opId()
                        + " but nothing is staged on this peer and the target "
                        + (apply.targetSha256() == null || apply.targetSha256().isBlank()
                        ? "(no SHA in APPLY payload)"
                        : apply.targetSha256().substring(0, Math.min(10, apply.targetSha256().length())))
                        + " is not in the local catalog. Skipping.");
                return;
            }
        }
        long delayMillis = Math.max(0L, apply.applyAt().toEpochMilli() - System.currentTimeMillis());
        long delayTicks = delayMillis / 50L;
        Runnable doApply = () -> Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                attemptApplyStaged();
                loaderLog.info("Applied fleet rollout (" + apply.opId().toString().substring(0, 8) + ").");
            } catch (Throwable t) {
                loaderLog.log(Level.WARNING, "Fleet APPLY for " + apply.opId() + " failed on this peer.", t);
            }
        });
        if (delayTicks <= 0) {
            doApply.run();
        } else {
            Bukkit.getScheduler().runTaskLater(this, doApply, delayTicks);
            long delaySeconds = Math.round(delayMillis / 1000.0);
            loaderLog.info("Applying staged rollout in " + delaySeconds + "s.");
        }
    }

    public LoaderCore core() {
        return core;
    }

    public PluginRuntime runtime() {
        return runtime;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("tgloader");
        if (command == null) return;
        BukkitTgLoaderCommand executor = new BukkitTgLoaderCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        Bukkit.getPluginManager().registerEvents(new TgLoaderCommandHider(), this);
    }
}
