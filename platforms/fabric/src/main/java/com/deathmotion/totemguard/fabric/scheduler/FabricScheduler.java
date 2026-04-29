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

package com.deathmotion.totemguard.fabric.scheduler;

import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.fabric.FabricServerHolder;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class FabricScheduler implements Scheduler {

    private final ScheduledExecutorService asyncExecutor;

    public FabricScheduler() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "totemguard-fabric-async-" + count.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        this.asyncExecutor = Executors.newScheduledThreadPool(2, threadFactory);
    }

    @Override
    public void runMainThreadTask(Runnable task) {
        MinecraftServer server = FabricServerHolder.server();
        if (server == null) {
            // Server not running; fall back to async so the task still completes.
            asyncExecutor.execute(task);
            return;
        }
        server.execute(task);
    }

    @Override
    public void runAsyncTask(Runnable task) {
        asyncExecutor.execute(task);
    }

    @Override
    public void runAsyncTaskDelayed(Runnable task, long delay, TimeUnit timeUnit) {
        asyncExecutor.schedule(task, delay, timeUnit);
    }

    @Override
    public ScheduledTask runAsyncTaskAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit timeUnit) {
        Future<?> handle = asyncExecutor.scheduleAtFixedRate(task, initialDelay, period, timeUnit);
        return () -> handle.cancel(false);
    }
}
