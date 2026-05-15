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

package com.deathmotion.totemguard.fabric;

import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.host.Platform;
import com.deathmotion.totemguard.host.TGPluginEntry;
import com.deathmotion.totemguard.host.TGPluginHandle;
import com.deathmotion.totemguard.host.TGPluginHost;
import org.jetbrains.annotations.NotNull;

/**
 * Loader entry point for Fabric. Started by the TotemGuard loader via the
 * {@code META-INF/services/com.deathmotion.totemguard.host.TGPluginEntry}
 * service file when this jar is selected as the inner plugin.
 * <p>
 * Note: Fabric does not deliver a {@code SERVER_STARTED} callback after the loader
 * has hot-loaded this entry, since registration happens after game launch. Startup
 * runs synchronously in {@link #start(TGPluginHost)} instead.
 */
public final class TGFabricEntry implements TGPluginEntry {

    @Override
    public @NotNull Platform platform() {
        return Platform.FABRIC;
    }

    @Override
    public @NotNull TGPluginHandle start(@NotNull TGPluginHost host) {
        TGFabricPlatform platform = new TGFabricPlatform(host.dataFolder());
        platform.setManagedByLoader(true);
        platform.setPluginHost(host);
        platform.commonOnEnable();
        if (!platform.isEnabled()) {
            throw new IllegalStateException("TotemGuard failed to enable; see preceding log entries.");
        }

        return new FabricHandle(platform);
    }

    private static final class FabricHandle implements TGPluginHandle {

        private final TGFabricPlatform platform;
        private volatile boolean stopped;

        FabricHandle(TGFabricPlatform platform) {
            this.platform = platform;
        }

        @Override
        public void stop(@NotNull TGPluginShutdownEvent.Reason reason) {
            if (stopped) return;
            stopped = true;
            platform.setShutdownReason(reason);
            platform.commonOnDisable();
        }

        @Override
        public @NotNull String version() {
            return TGVersions.CURRENT.toString();
        }
    }
}
