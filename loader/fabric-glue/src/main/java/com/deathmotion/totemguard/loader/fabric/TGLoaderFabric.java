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
import com.deathmotion.totemguard.loader.command.LoaderApp;
import com.deathmotion.totemguard.loader.core.*;
import com.deathmotion.totemguard.loader.fleet.RolloutCoordinator;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class TGLoaderFabric implements DedicatedServerModInitializer, LoaderApp {

    private final Logger logger = Logger.getLogger("TotemGuard-Loader");
    private final Set<Future<?>> inFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean disabling;
    private volatile LoaderPaths paths;
    private volatile LoaderCore core;
    private volatile PluginRuntime runtime;
    private volatile ExecutorService bootstrapWorker;
    private volatile ScheduledExecutorService delayScheduler;
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
    public void onInitializeServer() {
        bridgeLoggingToFabric();
        LoaderServerHolder.init();

        boolean dev = FabricLoader.getInstance().isDevelopmentEnvironment();
        if (!dev) {
            if (!new JarIntegrityChecker(logger, "TotemGuard-Loader").verifyCurrentJar()) {
                logger.severe("TotemGuard Loader will NOT load the TotemGuard plugin because the loader jar failed integrity verification.");
                return;
            }
        } else {
            logger.info("Dev environment detected, skipping loader jar integrity verification.");
        }
        logger.info("TotemGuard Loader " + LoaderManifest.loaderVersion() + " on Fabric");

        try {
            this.paths = LoaderPaths.forFabric(FabricLoader.getInstance().getConfigDir());
            this.core = new LoaderCore(logger, paths, HostPlatform.FABRIC);
            this.core.initRolloutCoordinator(this::onFleetApply);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "TotemGuard Loader could not prepare its data directory. "
                    + "The loader will stay enabled but inert. Check filesystem permissions.", t);
            return;
        }

        this.bootstrapWorker = Executors.newSingleThreadExecutor(new NamedThreadFactory("TotemGuard-Loader-Bootstrap"));
        this.delayScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("TotemGuard-Loader-Delay"));

        new FabricTgLoaderCommand(this).register();

        startupFuture = bootstrapWorker.submit(() -> {
            try {
                attemptStart(null);
            } catch (Throwable t) {
                if (disabling) return;
                logger.log(Level.SEVERE, "TotemGuard could not be loaded automatically ("
                        + describeRootCause(t) + "). The loader is still online. "
                        + "Drop a jar into " + paths.localDir() + " and run /tgloader load <version> from console.", t);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "TotemGuard-Loader-Shutdown"));
    }

    private void shutdown() {
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
        if (delayScheduler != null) {
            delayScheduler.shutdownNow();
        }
        if (runtime != null) {
            try {
                runtime.shutdown();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "TotemGuard Loader shutdown threw", t);
            }
        }
        if (core != null) {
            try {
                core.shutdown();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Loader core shutdown threw", t);
            }
        }
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
        return logger;
    }

    @Override
    public boolean isDisabling() {
        return disabling;
    }

    @Override
    public Future<?> submitBackground(Runnable task) {
        if (disabling || bootstrapWorker == null) {
            return CompletableFuture.completedFuture(null);
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
            LoaderResult result = core.run(getClass().getClassLoader(), versionOverride);
            PluginRuntime created = new PluginRuntime(core, this, paths, getClass().getClassLoader(), logger);
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
                staged = core.tryPullAndStageFromFleet(apply.targetVersion(), apply.targetSha256());
            }
            if (!staged) {
                logger.warning("Received fleet APPLY for " + apply.opId()
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
                logger.info("Applied fleet rollout (" + apply.opId().toString().substring(0, 8) + ").");
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Fleet APPLY for " + apply.opId() + " failed on this peer.", t);
                core.reportApplyFailure(apply.opId(), describeRootCause(t));
            }
        });
        if (delayMillis <= 0) {
            doApply.run();
        } else {
            delayScheduler.schedule(doApply, delayMillis, TimeUnit.MILLISECONDS);
            long delaySeconds = Math.round(delayMillis / 1000.0);
            logger.info("Applying staged rollout in " + delaySeconds + "s.");
        }
    }

    private void bridgeLoggingToFabric() {
        // Bridge the root JUL logger so the dynamically loaded plugin's per-class
        // loggers route through SLF4J instead of JUL's default two-line stderr format.
        Logger root = Logger.getLogger("");
        root.setUseParentHandlers(false);
        for (Handler h : root.getHandlers()) root.removeHandler(h);
        root.addHandler(new Slf4jBridgeHandler());
        root.setLevel(Level.ALL);
        logger.setUseParentHandlers(true);
        for (Handler h : logger.getHandlers()) logger.removeHandler(h);
        logger.setLevel(Level.ALL);
    }

    private static final class Slf4jBridgeHandler extends Handler {
        @Override
        public void publish(LogRecord r) {
            if (r == null) return;
            String name = r.getLoggerName();
            if (name == null || name.isEmpty()) name = "TotemGuard";
            org.slf4j.Logger target = org.slf4j.LoggerFactory.getLogger(name);
            String msg = r.getMessage();
            Object[] params = r.getParameters();
            if (msg != null && params != null && params.length > 0) {
                try {
                    msg = MessageFormat.format(msg, params);
                } catch (IllegalArgumentException ignored) {
                }
            }
            Throwable t = r.getThrown();
            int lvl = r.getLevel().intValue();
            if (lvl >= Level.SEVERE.intValue()) target.error(msg, t);
            else if (lvl >= Level.WARNING.intValue()) target.warn(msg, t);
            else if (lvl >= Level.INFO.intValue()) target.info(msg, t);
            else if (lvl >= Level.FINE.intValue()) target.debug(msg, t);
            else target.trace(msg, t);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
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
