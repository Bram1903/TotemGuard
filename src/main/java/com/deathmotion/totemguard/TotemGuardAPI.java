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

package com.deathmotion.totemguard;

import com.deathmotion.totemguard.api.ITotemGuardAPI;
import com.deathmotion.totemguard.api.interfaces.IAlertManager;
import com.deathmotion.totemguard.api.interfaces.IConfigManager;
import com.deathmotion.totemguard.util.TGVersions;
import org.bukkit.Bukkit;

public class TotemGuardAPI implements ITotemGuardAPI {

    private final TotemGuard plugin;

    public TotemGuardAPI(TotemGuard plugin) {
        this.plugin = plugin;
        registerService();
    }

    private void registerService() {
        Bukkit.getServicesManager().register(ITotemGuardAPI.class, this, plugin, org.bukkit.plugin.ServicePriority.Normal);
    }

    @Override
    public String getTotemGuardVersion() {
        return TGVersions.CURRENT.toString();
    }

    @Override
    public IAlertManager alertManager() {
        return plugin.getAlertManager();
    }

    @Override
    public IConfigManager configManager() {
        return plugin.getConfigManager();
    }
}
