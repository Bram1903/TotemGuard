/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.velocity.scheduler;

import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.velocity.TGVelocity;
import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;

public final class VelocityScheduler implements Scheduler {

    private final TGVelocity plugin;
    private final ProxyServer proxy;

    @Inject
    public VelocityScheduler(TGVelocity plugin, ProxyServer server) {
        this.plugin = plugin;
        this.proxy = server;
    }

    @Override
    public void runMainThreadTask(Runnable task) {
        this.proxy.getScheduler()
                .buildTask(this.plugin, task)
                .schedule();
    }

    @Override
    public void runAsyncTask(Runnable task) {
        this.proxy.getScheduler()
                .buildTask(this.plugin, task)
                .schedule();
    }

    @Override
    public void runAsyncTaskDelayed(Runnable task, long delay, TimeUnit timeUnit) {
        this.proxy.getScheduler()
                .buildTask(plugin, task)
                .delay(delay, timeUnit)
                .schedule();
    }
}