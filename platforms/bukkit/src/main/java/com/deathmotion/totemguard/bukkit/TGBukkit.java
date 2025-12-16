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

package com.deathmotion.totemguard.bukkit;

import com.deathmotion.totemguard.bukkit.player.BukkitPlatformUserFactory;
import com.deathmotion.totemguard.bukkit.sender.BukkitSenderFactory;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.TGVersions;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.Configurable;

@Getter
public class TGBukkit extends JavaPlugin {

    @Getter
    private static TGBukkit instance;

    private final TGBukkitPlatform tg = new TGBukkitPlatform(this);
    private BukkitSenderFactory senderFactory;
    private BukkitPlatformUserFactory bukkitPlatformUserFactory;
    private CommandManager<Sender> commandManager;

    public TGBukkit() {
        instance = this;
    }

    @Override
    public void onLoad() {
        tg.commonOnInitialize();
    }

    @Override
    public void onEnable() {
        senderFactory = new BukkitSenderFactory();
        bukkitPlatformUserFactory = new BukkitPlatformUserFactory();

        LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
            CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
            Configurable<BrigadierSetting> settings = cbm.settings();
            settings.set(BrigadierSetting.FORCE_EXECUTABLE, true);
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }

        commandManager = manager;

        tg.commonOnEnable();
    }

    @Override
    public void onDisable() {
        tg.commonOnDisable();
    }

    void enableBStats() {
        try {
            Metrics metrics = new Metrics(this, TGPlatform.getBStatsId());
            metrics.addCustomChart(new SimplePie("tg_version", TGVersions.CURRENT::toStringWithoutSnapshot));
            metrics.addCustomChart(new SimplePie("tg_platform", () -> "Bukkit"));
        } catch (Exception e) {
            tg.getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
        }
    }
}