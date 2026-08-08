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

package com.deathmotion.totemguard.host;

import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.api.host.LoaderInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface LoaderController {

    @NotNull LoaderInfo info();

    @NotNull UpdateTarget resolveTarget() throws IOException;

    byte @NotNull [] download(@NotNull UpdateTarget target) throws IOException;

    void stageJar(byte @NotNull [] bytes, @NotNull UpdateTarget target) throws IOException;

    @NotNull CompletableFuture<Void> restart(@NotNull TGPluginShutdownEvent.Reason reason);

    default @NotNull CompletableFuture<Void> restart() {
        return restart(TGPluginShutdownEvent.Reason.LOADER_RESTART);
    }

    @NotNull CompletableFuture<Void> stop(@NotNull TGPluginShutdownEvent.Reason reason);

    default @NotNull CompletableFuture<Void> stop() {
        return stop(TGPluginShutdownEvent.Reason.LOADER_STOP);
    }

    void attachFleetCache(@Nullable FleetCache cache);

    @Nullable FleetCache fleetCache();
}
