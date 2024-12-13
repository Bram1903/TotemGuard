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
import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.config.ConfigManager;
import com.deathmotion.totemguard.util.TGVersions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

public class TotemGuardAPI implements ITotemGuardAPI {

    private final TotemGuard plugin;
    private final ConfigManager configManager;

    protected TotemGuardAPI(TotemGuard plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();

        registerService();
    }

    private void registerService() {
        Bukkit.getServicesManager().register(ITotemGuardAPI.class, this, plugin, ServicePriority.Normal);
    }

    @Override
    public boolean isEnabled() {
        return configManager.getSettings().isApi();
    }

    @Override
    public String getServerName() {
        return isEnabled() ? configManager.getSettings().getServer() : null;
    }

    @Override
    public TGVersion getVersion() {
        return isEnabled() ? TGVersions.CURRENT : null;
    }

    @Override
    public IAlertManager getAlertManager() {
        return isEnabled() ? plugin.getAlertManager() : null;
    }

    @Override
    public IConfigManager getConfigManager() {
        return isEnabled() ? plugin.getConfigManager() : null;
    }
}