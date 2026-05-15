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

package com.deathmotion.totemguard.bukkit.scheduler;

import com.deathmotion.totemguard.common.util.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

final class PaperBukkitScheduler implements BukkitScheduler {

    private final Plugin plugin;
    private final org.bukkit.scheduler.BukkitScheduler scheduler;

    PaperBukkitScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }

    @Override
    public void runMainThreadTask(Runnable task) {
        scheduler.runTask(plugin, task);
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        scheduler.runTask(plugin, task);
    }

    @Override
    public void runForEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        scheduler.runTask(plugin, task);
    }

    @Override
    public void runAsyncTask(Runnable task) {
        scheduler.runTaskAsynchronously(plugin, task);
    }

    @Override
    public ScheduledTask runAsyncTaskDelayed(Runnable task, long delay, TimeUnit timeUnit) {
        long ticks = Math.max(1L, timeUnit.toMillis(delay) / 50L);
        BukkitTask bukkitTask = scheduler.runTaskLaterAsynchronously(plugin, task, ticks);
        return bukkitTask::cancel;
    }

    @Override
    public ScheduledTask runAsyncTaskAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit timeUnit) {
        long initialTicks = Math.max(1L, timeUnit.toMillis(initialDelay) / 50L);
        long periodTicks = Math.max(1L, timeUnit.toMillis(Math.max(1L, period)) / 50L);
        BukkitTask bukkitTask = scheduler.runTaskTimerAsynchronously(plugin, task, initialTicks, periodTicks);
        return bukkitTask::cancel;
    }
}
