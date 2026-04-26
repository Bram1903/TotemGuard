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

package com.deathmotion.totemguard.bungee.scheduler;

import com.deathmotion.totemguard.common.util.Scheduler;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class BungeeScheduler implements Scheduler {

    private final Plugin plugin;

    public BungeeScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runMainThreadTask(Runnable task) {
        // BungeeCord has no main thread; the scheduler always runs on a worker pool.
        plugin.getProxy().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runAsyncTask(Runnable task) {
        plugin.getProxy().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runAsyncTaskDelayed(Runnable task, long delay, TimeUnit timeUnit) {
        plugin.getProxy().getScheduler().schedule(plugin, task, delay, timeUnit);
    }
}
