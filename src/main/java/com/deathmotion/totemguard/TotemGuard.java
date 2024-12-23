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

import com.deathmotion.totemguard.events.packets.PacketConfigurationListener;
import com.deathmotion.totemguard.events.packets.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.manager.AlertManagerImpl;
import com.deathmotion.totemguard.manager.ConfigManager;
import com.deathmotion.totemguard.manager.PlayerDataManager;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    private final ConfigManager configManager = new ConfigManager(this);
    private final MessengerService messengerService = new MessengerService(this);
    private final AlertManagerImpl alertManager = new AlertManagerImpl(this);
    private final PlayerDataManager playerDataManager = new PlayerDataManager();

    @Override
    public void onEnable() {
        instance = this;

        PacketEvents.getAPI().getEventManager().registerListener(new PacketConfigurationListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerJoinQuit());
    }

    @Override
    public void onDisable() {

    }
}
