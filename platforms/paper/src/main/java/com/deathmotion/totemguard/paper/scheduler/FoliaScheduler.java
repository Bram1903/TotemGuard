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

import com.deathmotion.totemguard.common.util.ScheduledTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

final class FoliaScheduler implements PaperScheduler {

    private final Plugin plugin;
    private final GlobalRegionScheduler globalScheduler;
    private final AsyncScheduler asyncScheduler;

    FoliaScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.globalScheduler = Bukkit.getGlobalRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }

    @Override
    public void runMainThreadTask(Runnable task) {
        globalScheduler.run(plugin, t -> task.run());
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        globalScheduler.run(plugin, t -> task.run());
    }

    @Override
    public void runForEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        entity.getScheduler().run(plugin, t -> task.run(), retired);
    }

    @Override
    public void runAsyncTask(Runnable task) {
        asyncScheduler.runNow(plugin, t -> task.run());
    }

    @Override
    public ScheduledTask runAsyncTaskDelayed(Runnable task, long delay, TimeUnit timeUnit) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                asyncScheduler.runDelayed(plugin, t -> task.run(), delay, timeUnit);
        return scheduled::cancel;
    }

    @Override
    public ScheduledTask runAsyncTaskAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit timeUnit) {
        long periodClamped = Math.max(1L, period);
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                asyncScheduler.runAtFixedRate(plugin, t -> task.run(),
                        Math.max(0L, initialDelay), periodClamped, timeUnit);
        return scheduled::cancel;
    }
}
