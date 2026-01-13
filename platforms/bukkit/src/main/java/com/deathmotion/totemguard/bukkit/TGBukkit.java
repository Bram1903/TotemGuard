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

import com.deathmotion.totemguard.bukkit.placeholder.PlaceholderAPIHolder;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.util.TGVersions;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class TGBukkit extends JavaPlugin {

    @Getter
    private static TGBukkit instance;

    private TGBukkitPlatform tg;

    @Override
    public void onLoad() {
        instance = this;

        tg = new TGBukkitPlatform(this);
        tg.commonOnInitialize();
    }

    @Override
    public void onEnable() {
        tg.commonOnEnable();
        if (!tg.isEnabled()) return;

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            tg.getPlaceholderRepository().registerHolder(new PlaceholderAPIHolder());
        }
    }

    @Override
    public void onDisable() {
        if (tg != null) {
            tg.commonOnDisable();
        }
    }

    void enableBStats() {
        try {
            Metrics metrics = new Metrics(this, TGPlatform.getBStatsId());
            metrics.addCustomChart(new SimplePie("tg_version", TGVersions.CURRENT::toStringWithoutSnapshot));
            metrics.addCustomChart(new SimplePie("tg_platform", () -> "Bukkit"));
        } catch (Exception e) {
            if (tg != null) {
                tg.getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
            } else {
                getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
            }
        }
    }
}
