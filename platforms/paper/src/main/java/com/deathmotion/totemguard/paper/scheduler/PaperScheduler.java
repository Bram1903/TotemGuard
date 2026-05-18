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

package com.deathmotion.totemguard.paper.scheduler;

import com.deathmotion.totemguard.common.util.Scheduler;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper-flavoured scheduler. Extends the platform-agnostic {@link Scheduler} with the
 * region-aware operations Folia requires (global region, entity-owned tasks).
 * <p>
 * One concrete impl is picked at platform construction time via {@link #create(Plugin)};
 * after that, calls dispatch through a single virtual lookup with no per-task Folia check.
 */
public interface PaperScheduler extends Scheduler {

    /**
     * Detects Folia once and returns the matching implementation. Folia removes the legacy
     * Paper scheduler, so this selection has to happen up front.
     */
    static @NotNull PaperScheduler create(@NotNull Plugin plugin) {
        if (isFolia()) {
            return new FoliaScheduler(plugin);
        }
        return new PaperLegacyScheduler(plugin);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Runs {@code task} on the global region (Folia) or the main server thread (Paper).
     */
    void runGlobal(@NotNull Runnable task);

    /**
     * Runs {@code task} on the region owning {@code entity} (Folia) or the main server thread
     * (Paper). {@code retired} is invoked instead if the entity has been removed before the
     * task fires; ignored on Paper.
     */
    void runForEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired);
}
