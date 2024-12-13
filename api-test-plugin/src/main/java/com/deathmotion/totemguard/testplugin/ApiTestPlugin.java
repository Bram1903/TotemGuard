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
import com.deathmotion.totemguard.testplugin.commands.AlertsCommand;
import com.deathmotion.totemguard.testplugin.events.ApiDisabledEventTest;
import com.deathmotion.totemguard.testplugin.events.ApiEnabledEventTest;
import com.deathmotion.totemguard.testplugin.events.FlagEventTest;
import com.deathmotion.totemguard.testplugin.events.PunishEventTest;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

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

        if (!api.isApiEnabled()) {
            getLogger().severe("TotemGuard API is not enabled.");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        registerListeners();
        registerCommand("alerts", new AlertsCommand(this));

        getLogger().info("Successfully hooked into TotemGuard API version " + api.getTotemGuardVersion().toString() + ".");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ApiEnabledEventTest(this), this);
        Bukkit.getPluginManager().registerEvents(new ApiDisabledEventTest(this), this);
        Bukkit.getPluginManager().registerEvents(new FlagEventTest(this), this);
        Bukkit.getPluginManager().registerEvents(new PunishEventTest(this), this);
    }

    private void registerCommand(String name, Command command) {
        try {
            // Get the CommandMap instance
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // Register the command
            commandMap.register(name, command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
