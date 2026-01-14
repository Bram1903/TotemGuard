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

package com.deathmotion.totemguard.bukkit;

import com.deathmotion.totemguard.bukkit.player.BukkitPlatformUserFactory;
import com.deathmotion.totemguard.bukkit.scheduler.BukkitScheduler;
import com.deathmotion.totemguard.bukkit.sender.BukkitSenderFactory;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformUserFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.Scheduler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.Configurable;

@Getter
public class TGBukkitPlatform extends TGPlatform {

    private final TGBukkit plugin;
    private final Scheduler scheduler;

    private final Lazy<BukkitSenderFactory> senderFactory;
    private final Lazy<BukkitPlatformUserFactory> platformUserFactory;
    private final Lazy<CommandManager<Sender>> commandManager;

    public TGBukkitPlatform(TGBukkit plugin) {
        super(Platform.PAPER);
        this.plugin = plugin;
        this.scheduler = new BukkitScheduler(plugin);

        this.senderFactory = Lazy.of(BukkitSenderFactory::new);
        this.platformUserFactory = Lazy.of(BukkitPlatformUserFactory::new);

        this.commandManager = Lazy.of(() -> {
            LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                    plugin,
                    ExecutionCoordinator.simpleCoordinator(),
                    senderFactory.get()
            );

            if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                manager.registerBrigadier();
                CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
                Configurable<BrigadierSetting> settings = cbm.settings();
                settings.set(BrigadierSetting.FORCE_EXECUTABLE, true);
            } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                manager.registerAsynchronousCompletions();
            }

            return manager;
        });
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public org.incendo.cloud.CommandManager<Sender> getCommandManager() {
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
        return plugin.getDataFolder().getAbsolutePath();
    }

    @Override
    public String getPlatformVersion() {
        return Bukkit.getMinecraftVersion();
    }

    @Override
    public boolean isPluginEnabled(String plugin) {
        return Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    @Override
    public void disablePlugin() {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }
}
