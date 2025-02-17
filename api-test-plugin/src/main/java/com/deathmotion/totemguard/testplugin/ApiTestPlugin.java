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

package com.deathmotion.totemguard.testplugin;

import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.TotemGuardProvider;
import com.deathmotion.totemguard.testplugin.events.AlertsToggleEventTest;
import com.deathmotion.totemguard.testplugin.events.FlagEventTest;
import com.deathmotion.totemguard.testplugin.events.PunishEventTest;
import com.deathmotion.totemguard.testplugin.events.UpdateFoundEventTest;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ApiTestPlugin extends JavaPlugin {

    TotemGuardAPI api;

    @Override
    public void onEnable() {
        if (!(getServer().getPluginManager().isPluginEnabled("TotemGuard"))) {
            getLogger().severe("TotemGuard is not enabled! This plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api = TotemGuardProvider.getAPI();

        Bukkit.getPluginManager().registerEvents(new FlagEventTest(this), this);
        Bukkit.getPluginManager().registerEvents(new PunishEventTest(this), this);
        Bukkit.getPluginManager().registerEvents(new AlertsToggleEventTest(this), this);
        Bukkit.getPluginManager().registerEvents(new UpdateFoundEventTest(this), this);

        getLogger().info("Successfully hooked into TotemGuard v" + api.getVersion());
    }

    @Override
    public void onDisable() {
    }
}
