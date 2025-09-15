/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2025 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.example;

import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.TotemGuardProvider;
import com.deathmotion.totemguard.example.events.AlertsToggleEventExample;
import com.deathmotion.totemguard.example.events.FlagEventExample;
import com.deathmotion.totemguard.example.events.PunishEventExample;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class TotemGuardApiExample extends JavaPlugin {

    TotemGuardAPI api;

    @Override
    public void onEnable() {
        if (!(getServer().getPluginManager().isPluginEnabled("TotemGuard"))) {
            getLogger().severe("TotemGuard is not enabled! This plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api = TotemGuardProvider.getAPI();

        Bukkit.getPluginManager().registerEvents(new FlagEventExample(this), this);
        Bukkit.getPluginManager().registerEvents(new PunishEventExample(this), this);
        Bukkit.getPluginManager().registerEvents(new AlertsToggleEventExample(this), this);

        getLogger().info("Successfully hooked into TotemGuard v" + api.getVersion());
    }

    @Override
    public void onDisable() {
    }
}
