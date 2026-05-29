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

package com.deathmotion.totemguard.common.loader;

import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.api.host.LoaderInfo;
import com.deathmotion.totemguard.api.loader.LoaderControl;
import com.deathmotion.totemguard.api.loader.UpdateResult;
import com.deathmotion.totemguard.host.LoaderController;
import com.deathmotion.totemguard.host.UpdateTarget;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Public-API adapter over the loader's internal {@link LoaderController}. Translates the
 * loader/host contract (which third parties must not import directly) into the published
 * {@link LoaderControl} surface.
 */
public final class LoaderControlAdapter implements LoaderControl {

    private final LoaderController controller;

    public LoaderControlAdapter(@NotNull LoaderController controller) {
        this.controller = controller;
    }

    @Override
    public @NotNull LoaderInfo info() {
        return controller.info();
    }

    @Override
    public @NotNull CompletableFuture<Void> restart() {
        return controller.restart(TGPluginShutdownEvent.Reason.LOADER_RESTART);
    }

    @Override
    public @NotNull CompletableFuture<UpdateResult> update() {
        return CompletableFuture.supplyAsync(this::resolveAndStage)
                .thenCompose(staged -> {
                    if (staged.outcome() != UpdateResult.Outcome.UPDATING) {
                        return CompletableFuture.completedFuture(staged);
                    }
                    return controller.restart(TGPluginShutdownEvent.Reason.UPDATE_TRIGGERED)
                            .thenApply(ignored -> staged);
                });
    }

    private UpdateResult resolveAndStage() {
        try {
            UpdateTarget target = controller.resolveTarget();
            if (target.version().equals(controller.info().loadedVersion())) {
                return new UpdateResult(UpdateResult.Outcome.UP_TO_DATE, target.version(),
                        "Already running the latest version (" + target.version() + ").");
            }
            controller.stageJar(controller.download(target), target);
            return new UpdateResult(UpdateResult.Outcome.UPDATING, target.version(),
                    "Staged " + target.version() + "; restarting now.");
        } catch (IOException e) {
            return new UpdateResult(UpdateResult.Outcome.FAILED, null,
                    "Update failed: " + e.getMessage());
        }
    }
}
