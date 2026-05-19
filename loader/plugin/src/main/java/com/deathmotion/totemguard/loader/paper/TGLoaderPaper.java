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

package com.deathmotion.totemguard.loader.paper;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.loader.command.LoaderApp;
import com.deathmotion.totemguard.loader.core.*;
import com.deathmotion.totemguard.loader.fleet.RolloutCoordinator;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TGLoaderPaper extends JavaPlugin implements LoaderApp {

    private final Logger loaderLog = Logger.getLogger("TotemGuard-Loader");
    private final Set<Future<?>> inFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean integrityFailed;
    private volatile boolean disabling;
    private volatile LoaderPaths paths;
    private volatile LoaderCore core;
    private volatile PluginRuntime runtime;
    private volatile ExecutorService bootstrapWorker;
    private volatile Future<?> startupFuture;

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
            this.paths = LoaderPaths.forPaper(getDataFolder().toPath());
            this.core = new LoaderCore(loaderLog, paths, HostPlatform.PAPER);
            this.core.initRolloutCoordinator(this::onFleetApply);
        } catch (Throwable t) {
            loaderLog.log(Level.SEVERE, "TotemGuard Loader could not prepare its data directory. "
                    + "The loader will stay enabled but inert. Check filesystem permissions.", t);
            return;
        }

        this.bootstrapWorker = Executors.newSingleThreadExecutor(new NamedThreadFactory("TotemGuard-Loader-Bootstrap"));
        registerCommand();

        // Off the main thread so a slow resolve/download can't stall Paper boot.
        startupFuture = bootstrapWorker.submit(() -> {
            try {
                attemptStart(null);
            } catch (Throwable t) {
                if (disabling) return;
                loaderLog.log(Level.SEVERE, "TotemGuard could not be loaded automatically ("
                        + describeRootCause(t) + "). The loader is still enabled. "
                        + "Recover with /tgloader load <version>, or drop a jar into "
                        + paths.localDir() + " then run /tgloader import followed by "
                        + "/tgloader load <version>.", t);
            }
        });
    }

    @Override
    public void onDisable() {
        disabling = true;
        if (startupFuture != null) {
            startupFuture.cancel(true);
        }
        for (Future<?> f : inFlight) {
            f.cancel(true);
        }
        if (bootstrapWorker != null) {
            bootstrapWorker.shutdownNow();
            try {
                bootstrapWorker.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
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
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean isDisabling() {
        return disabling;
    }

    @Override
    public LoaderCore core() {
        return core;
    }

    @Override
    public PluginRuntime runtime() {
        return runtime;
    }

    @Override
    public Logger logger() {
        return loaderLog;
    }

    /**
     * Submit work to the loader's background bootstrap pool. Callers use this instead
     * of {@code Bukkit.getScheduler().runTaskAsynchronously} when they want their task
     * tracked (and cancelled on disable) so a long-running apply does not race against
     * server shutdown.
     */
    @Override
    public Future<?> submitBackground(Runnable task) {
        if (disabling || bootstrapWorker == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        Future<?>[] holder = new Future<?>[1];
        Future<?> f = bootstrapWorker.submit(() -> {
            try {
                task.run();
            } finally {
                if (holder[0] != null) inFlight.remove(holder[0]);
            }
        });
        holder[0] = f;
        inFlight.add(f);
        return f;
    }

    @Override
    public synchronized void attemptStart(String versionOverride) throws Exception {
        if (disabling) return;
        if (runtime == null) {
            LoaderResult result = core.run(getClassLoader(), versionOverride);
            PluginRuntime created = new PluginRuntime(core, this, paths, getClassLoader(), loaderLog);
            created.start(result.pluginJar());
            this.runtime = created;
        } else {
            runtime.loadVersionForCommand(versionOverride);
        }
    }

    @Override
    public LoaderCore.StageResult attemptStage(String versionOverride) throws Exception {
        return core.stageVersion(versionOverride);
    }

    @Override
    public synchronized void attemptApplyStaged() throws Exception {
        if (disabling) return;
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
        if (disabling) return;
        if (core.readStaged().isEmpty()) {
            boolean staged = core.stageFromCatalogBySha(apply.targetVersion(), apply.targetSha256());
            if (!staged) {
                // Try to pull the jar from the fleet blob cache (jar-available may have
                // arrived after we missed it, or this peer joined late). If still nothing,
                // surface the failure so operators see a partial rollout.
                staged = core.tryPullAndStageFromFleet(apply.targetVersion(), apply.targetSha256());
            }
            if (!staged) {
                loaderLog.warning("Received fleet APPLY for " + apply.opId()
                        + " but target "
                        + (apply.targetSha256() == null || apply.targetSha256().isBlank()
                        ? "(no SHA in APPLY payload)"
                        : apply.targetSha256().substring(0, Math.min(10, apply.targetSha256().length())))
                        + " is not available on this peer. Skipping.");
                core.reportApplyFailure(apply.opId(), "target not found");
                return;
            }
        }
        long delayMillis = Math.max(0L, apply.applyAt().toEpochMilli() - System.currentTimeMillis());
        Runnable doApply = () -> submitBackground(() -> {
            if (disabling) return;
            try {
                attemptApplyStaged();
                loaderLog.info("Applied fleet rollout (" + apply.opId().toString().substring(0, 8) + ").");
            } catch (Throwable t) {
                loaderLog.log(Level.WARNING, "Fleet APPLY for " + apply.opId() + " failed on this peer.", t);
                core.reportApplyFailure(apply.opId(), describeRootCause(t));
            }
        });
        if (delayMillis <= 0) {
            doApply.run();
        } else {
            long delayTicks = delayMillis / 50L;
            Bukkit.getScheduler().runTaskLater(this, doApply, delayTicks);
            long delaySeconds = Math.round(delayMillis / 1000.0);
            loaderLog.info("Applying staged rollout in " + delaySeconds + "s.");
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("tgloader");
        if (command == null) return;
        PaperTgLoaderCommand executor = new PaperTgLoaderCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        Bukkit.getPluginManager().registerEvents(new TgLoaderCommandHider(), this);
    }

    private static final class NamedThreadFactory implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger id = new AtomicInteger();
        private final String prefix;

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + id.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
