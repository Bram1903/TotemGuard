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

package com.deathmotion.totemguard.velocity;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformUserFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.velocity.player.VelocityPlatformUserFactory;
import com.deathmotion.totemguard.velocity.scheduler.VelocityScheduler;
import com.deathmotion.totemguard.velocity.sender.VelocitySenderFactory;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;

public class TGVelocityPlatform extends TGPlatform {

    private final TGVelocity plugin;
    private final VelocityScheduler scheduler;

    private final Lazy<VelocitySenderFactory> senderFactory;
    private final Lazy<VelocityPlatformUserFactory> platformUserFactory;
    private final Lazy<CommandManager<Sender>> commandManager;

    public TGVelocityPlatform(TGVelocity plugin) {
        super(Platform.VELOCITY);
        this.plugin = plugin;
        this.scheduler = new VelocityScheduler(plugin, plugin.getServer());

        this.senderFactory = Lazy.of(VelocitySenderFactory::new);
        this.platformUserFactory = Lazy.of(() -> new VelocityPlatformUserFactory(plugin.getServer()));

        this.commandManager = Lazy.of(() -> new VelocityCommandManager<>(
                plugin.getPluginContainer(),
                plugin.getServer(),
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        ));
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public CommandManager<Sender> getCommandManager() {
        return commandManager.get();
    }

    @Override
    public void enableBStats() {
        plugin.enableBStats();
    }

    @Override
    public PlatformUserFactory getPlatformUserFactory() {
        return platformUserFactory.get();
    }

    @Override
    public String getPluginDirectory() {
        return plugin.getDataDirectory().toAbsolutePath().toString();
    }

    @Override
    public String getPlatformVersion() {
        final String raw = plugin.getServer().getVersion().getVersion();
        final String s = raw.trim();
        final int space = s.indexOf(' ');
        return (space == -1) ? s : s.substring(0, space);
    }

    @Override
    public boolean isPluginEnabled(String plugin) {
        return this.plugin.getServer().getPluginManager().isLoaded(plugin);
    }

    @Override
    public void disablePlugin() {
        // Velocity has no general "disable this plugin" API.
        // commonOnEnable() will set enabled=false to prevent further operations.
    }
}

