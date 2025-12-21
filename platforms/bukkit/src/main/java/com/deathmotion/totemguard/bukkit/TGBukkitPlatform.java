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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.player.PlatformUserFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class TGBukkitPlatform extends TGPlatform {

    private final TGBukkit plugin;

    public TGBukkitPlatform(TGBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public org.incendo.cloud.CommandManager<Sender> getCommandManager() {
        return plugin.getCommandManager();
    }

    @Override
    public void enableBStats() {
        plugin.enableBStats();
    }

    @Override
    public PlatformUserFactory getPlatformUserFactory() {
        return plugin.getBukkitPlatformUserFactory();
    }

    @Override
    public String getPluginDirectory() {
        return this.plugin.getDataFolder().getAbsolutePath();
    }

    @Override
    public void disablePlugin() {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }
}