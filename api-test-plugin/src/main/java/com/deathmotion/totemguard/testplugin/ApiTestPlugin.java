/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

import com.deathmotion.totemguard.api.ITotemGuardAPI;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ApiTestPlugin extends JavaPlugin {

    ITotemGuardAPI api;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<ITotemGuardAPI> provider = Bukkit.getServicesManager().getRegistration(ITotemGuardAPI.class);
        if (provider != null) {
            api = provider.getProvider();
            getLogger().info("TotemGuard API provider found.");
        } else {
            getLogger().severe("Could not find TotemGuard API provider.");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        getLogger().info("Successfully hooked into TotemGuard API.");
    }

    @Override
    public void onDisable() {
    }
}
