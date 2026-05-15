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
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Standalone entry point for the TotemGuard plugin. When the inner jar is installed
 * directly (no loader), Bukkit loads this class as the registered plugin and drives
 * the lifecycle. The loader path bypasses this class entirely and instantiates
 * {@link TGBukkitPlatform} against its own JavaPlugin via {@link TGBukkitEntry}.
 */
@Getter
public class TGBukkit extends JavaPlugin {

    private TGBukkitPlatform tg;

    @Override
    public void onLoad() {
        tg = new TGBukkitPlatform(this);
    }

    @Override
    public void onEnable() {
        tg.commonOnEnable();
        if (!tg.isEnabled()) return;

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            tg.getPlaceholderRepository().registerHolder(new PlaceholderAPIHolder());
            tg.getNetworkPresenceRepository().reloadServerName();
        }
    }

    @Override
    public void onDisable() {
        if (tg != null) {
            tg.commonOnDisable();
        }
    }
}
