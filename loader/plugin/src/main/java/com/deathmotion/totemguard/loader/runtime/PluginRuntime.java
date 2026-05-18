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

import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.host.Platform;
import com.deathmotion.totemguard.host.TGPluginEntry;
import com.deathmotion.totemguard.host.TGPluginHandle;
import com.deathmotion.totemguard.host.TGPluginHost;
import com.deathmotion.totemguard.loader.classloader.TGPluginClassLoader;
import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.core.LoaderCore;
import com.deathmotion.totemguard.loader.core.LoaderPaths;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the live TotemGuard plugin: holds its {@link TGPluginClassLoader}, the API
 * {@link TGPluginHandle}, and drives start / restart / stop.
 * <p>
 * Reload uses a prepare-then-swap pattern. {@link #prepareNext} does everything that
 * does not require the running plugin to be stopped: resolve, download, integrity-
 * check, classloader creation, and ServiceLoader entry-point lookup. Only once that
 * succeeds does {@link #loadVersion} call {@link #stopCurrent} followed by
 * {@link #commitPrepared}. This narrows the window during which TotemGuard is not
 * active on the server to the time it takes the plugin's {@code start()} to return.
 * <p>
 * The API classes ({@link TGPluginHost}, {@link TGPluginEntry}, etc.) are injected
 * into the host classloader by {@code ApiClassInjector} before this runtime is
 * constructed, so the direct references below resolve at first method invocation.
 */
public final class PluginRuntime {

    private final LoaderCore core;
    private final Object nativePlugin;
    private final LoaderPaths paths;
    private final ClassLoader hostClassLoader;
    private final Logger logger;
    private final LoaderControllerImpl loaderController;

    private TGPluginClassLoader pluginClassLoader;
    private TGPluginHandle handle;
    private String loadedVersion;

    public PluginRuntime(LoaderCore core, Object nativePlugin, LoaderPaths paths,
                         ClassLoader hostClassLoader, Logger logger) {
        this.core = core;
        this.nativePlugin = nativePlugin;
        this.paths = paths;
        this.hostClassLoader = hostClassLoader;
        this.logger = logger;
        this.loaderController = new LoaderControllerImpl(core, this);
    }

    public synchronized void start(Path pluginJar) throws Exception {
        if (handle != null) {
            logger.warning("PluginRuntime.start() called while a plugin is already loaded. Skipping.");
            return;
        }
        PreparedPlugin prepared = prepareFromJar(pluginJar);
        commitPrepared(prepared, null);
    }

    public synchronized void loadVersion(String versionOverride, TGPluginShutdownEvent.Reason reason) throws Exception {
        PreparedPlugin prepared = prepareNext(versionOverride);
        try {
            if (handle != null) {
                stopCurrent(reason);
            }
            commitPrepared(prepared, reason);
        } catch (Throwable t) {
            safeClose(prepared.classLoader);
            throw t;
        }
    }

    public void loadVersionForCommand(String versionOverride) throws Exception {
        loadVersion(versionOverride, TGPluginShutdownEvent.Reason.LOADER_RESTART);
    }

    public void stopForCommand() {
        stop(TGPluginShutdownEvent.Reason.LOADER_STOP);
    }

    public synchronized boolean isLoaded() {
        return handle != null;
    }

    public synchronized void stop(TGPluginShutdownEvent.Reason reason) {
        if (handle == null) return;
        try {
            stopCurrent(reason);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "PluginRuntime stop failed", t);
        }
    }

    public synchronized void restart() throws Exception {
        restart(TGPluginShutdownEvent.Reason.LOADER_RESTART);
    }

    public synchronized void restart(TGPluginShutdownEvent.Reason reason) throws Exception {
        loadVersion(null, reason);
    }

    public synchronized void shutdown() {
        if (handle == null) return;
        try {
            stopCurrent(TGPluginShutdownEvent.Reason.SERVER_STOP);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "PluginRuntime shutdown failed", t);
        }
    }

    public synchronized String loadedVersion() {
        return loadedVersion;
    }

    public LoaderPaths paths() {
        return paths;
    }

    public LoaderCore core() {
        return core;
    }

    private void stopCurrent(TGPluginShutdownEvent.Reason reason) {
        if (handle != null) {
            try {
                handle.stop(reason);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "TotemGuard plugin stop() threw", t);
            }
        }
        if (pluginClassLoader != null) {
            safeClose(pluginClassLoader);
        }
        handle = null;
        pluginClassLoader = null;
        loadedVersion = null;
    }

    private PreparedPlugin prepareNext(String versionOverride) throws Exception {
        Path pluginJar = core.run(hostClassLoader, versionOverride).pluginJar();
        return prepareFromJar(pluginJar);
    }

    private PreparedPlugin prepareFromJar(Path pluginJar) throws Exception {
        TGPluginClassLoader child = new TGPluginClassLoader(pluginJar, hostClassLoader);
        TGPluginEntry entry;
        try {
            entry = pickEntry(child);
        } catch (Throwable t) {
            safeClose(child);
            throw t;
        }
        if (entry == null) {
            safeClose(child);
            throw new IllegalStateException("TotemGuard plugin jar exposes no TGPluginEntry for " + core.platform());
        }
        return new PreparedPlugin(child, entry, pluginJar);
    }

    private void commitPrepared(PreparedPlugin prepared, TGPluginShutdownEvent.Reason reasonHint) throws Exception {
        HostPlatform hostPlatform = core.platform();
        Platform platform = Platform.valueOf(hostPlatform.name());
        PluginHost host = new PluginHost(platform, nativePlugin, logger,
                paths.pluginDataFolder(), hostClassLoader, loaderController);

        long startNanos = System.nanoTime();
        TGPluginHandle started;
        try {
            started = prepared.entry.start(host);
        } catch (Throwable t) {
            safeClose(prepared.classLoader);
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            throw new IllegalStateException("TotemGuard plugin failed to start: " + cause.getMessage(), cause);
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        String version;
        try {
            version = started.version();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "TGPluginHandle.version() threw; reporting as 'unknown'", t);
            version = "unknown";
        }

        this.pluginClassLoader = prepared.classLoader;
        this.handle = started;
        this.loadedVersion = version;

        logger.info("Loaded TotemGuard " + version + " in " + elapsedMs + "ms"
                + (reasonHint != null ? " (reason: " + reasonHint + ")" : ""));
    }

    private TGPluginEntry pickEntry(ClassLoader child) {
        Platform expected = Platform.valueOf(core.platform().name());

        ServiceLoader<TGPluginEntry> services = ServiceLoader.load(TGPluginEntry.class, child);
        for (TGPluginEntry candidate : services) {
            try {
                if (candidate.platform() == expected) return candidate;
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to query TGPluginEntry.platform()", t);
            }
        }
        return null;
    }

    private void safeClose(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to close " + closeable.getClass().getSimpleName(), t);
        }
    }

    private record PreparedPlugin(TGPluginClassLoader classLoader, TGPluginEntry entry, Path jar) {
    }
}
