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

package com.deathmotion.totemguard.common;

import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventBusImpl;
import com.deathmotion.totemguard.common.event.internal.InternalEventBus;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.permission.PermissionNode;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.replay.HeadlessLogging;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class HeadlessPlatform extends TGPlatform {

    private final Path directory;
    private final InlineScheduler scheduler = new InlineScheduler();

    public HeadlessPlatform(Path directory) {
        super(Platform.PAPER);
        this.directory = directory;
        HeadlessLogging.configure(getLogger());

        this.configRepository = new ConfigRepositoryImpl();
        this.placeholderRepository = new PlaceholderRepositoryImpl();
        this.messageService = new MessageService();
        this.eventBus = new EventBusImpl();
        this.internalEventBus = new InternalEventBus();
        this.guiManager = new GuiManager();
    }

    public long platformTouches() {
        return scheduler.touches.get();
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void dispatchCommand(String command) {
        scheduler.touches.incrementAndGet();
    }

    @Override
    public @Nullable Sender createSender(@NotNull UUID playerUuid) {
        return null;
    }

    @Override
    public CommandManager<Sender> getCommandManager() {
        throw new UnsupportedOperationException("headless replay has no command surface");
    }

    @Override
    public void enableBStats() {
    }

    @Override
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return uuid -> null;
    }

    @Override
    public String getPluginDirectory() {
        return directory.toString();
    }

    @Override
    public String getPlatformVersion() {
        return "headless";
    }

    @Override
    public boolean isPluginEnabled(String plugin) {
        return false;
    }

    @Override
    public void registerPermission(@NotNull PermissionNode node) {
    }

    @Override
    public void disablePlugin() {
    }

    @Override
    public boolean checkPlatformCompatibility() {
        return true;
    }

    @Override
    public boolean shouldVerifyJarIntegrity() {
        return false;
    }

    private static final class InlineScheduler implements Scheduler {

        private final AtomicLong touches = new AtomicLong();

        @Override
        public void runMainThreadTask(Runnable task) {
            touches.incrementAndGet();
            task.run();
        }

        @Override
        public void runAsyncTask(Runnable task) {
            touches.incrementAndGet();
            task.run();
        }

        @Override
        public ScheduledTask runAsyncTaskDelayed(Runnable task, long delay, TimeUnit timeUnit) {
            touches.incrementAndGet();
            return () -> {
            };
        }

        @Override
        public ScheduledTask runAsyncTaskAtFixedRate(Runnable task, long initialDelay, long period,
                                                     TimeUnit timeUnit) {
            touches.incrementAndGet();
            return () -> {
            };
        }
    }
}
