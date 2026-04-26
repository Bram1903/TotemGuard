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

package com.deathmotion.totemguard.bungee;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.util.TGVersions;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

@Getter
public final class TGBungee extends Plugin {

    @Getter
    private static TGBungee instance;

    private TGBungeePlatform tg;

    @Override
    public void onLoad() {
        instance = this;
        tg = new TGBungeePlatform(this);
        tg.commonOnInitialize();
    }

    @Override
    public void onEnable() {
        tg.commonOnEnable();
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
            metrics.addCustomChart(new SimplePie("tg_platform", () -> "BungeeCord"));
        } catch (Exception e) {
            getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
        }
    }
}
