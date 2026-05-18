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

package com.deathmotion.totemguard.paper;

import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.host.Platform;
import com.deathmotion.totemguard.host.TGPluginEntry;
import com.deathmotion.totemguard.host.TGPluginHandle;
import com.deathmotion.totemguard.host.TGPluginHost;
import com.deathmotion.totemguard.paper.placeholder.PlaceholderAPIHolder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Loader entry point. Started by the TotemGuard loader via the
 * {@code META-INF/services/com.deathmotion.totemguard.host.TGPluginEntry}
 * service file when this jar is selected as the inner plugin.
 */
public final class TGPaperEntry implements TGPluginEntry {

    @Override
    public @NotNull Platform platform() {
        return Platform.PAPER;
    }

    @Override
    public @NotNull TGPluginHandle start(@NotNull TGPluginHost host) {
        Object native_ = host.nativePlugin();
        if (!(native_ instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException(
                    "TGPaperEntry requires a JavaPlugin nativePlugin; got " + native_.getClass().getName());
        }

        TGPaperPlatform platform = new TGPaperPlatform(javaPlugin);
        platform.setManagedByLoader(true);
        platform.setPluginHost(host);
        platform.commonOnEnable();
        if (!platform.isEnabled()) {
            throw new IllegalStateException("TotemGuard failed to enable; see preceding log entries.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            platform.getPlaceholderRepository().registerHolder(new PlaceholderAPIHolder());
            platform.getNetworkPresenceRepository().reloadServerName();
        }

        return new PaperHandle(platform);
    }

    private static final class PaperHandle implements TGPluginHandle {

        private final TGPaperPlatform platform;
        private volatile boolean stopped;

        PaperHandle(TGPaperPlatform platform) {
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
