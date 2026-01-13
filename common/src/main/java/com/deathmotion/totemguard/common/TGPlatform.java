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

package com.deathmotion.totemguard.common;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.common.alert.AlertManagerImpl;
import com.deathmotion.totemguard.common.commands.CommandManagerImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.event.internal.listeners.EventCheckManagerListener;
import com.deathmotion.totemguard.common.event.internal.listeners.TotemReplenishedListener;
import com.deathmotion.totemguard.common.event.packet.PacketCheckManagerListener;
import com.deathmotion.totemguard.common.event.packet.PacketPingListener;
import com.deathmotion.totemguard.common.event.packet.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformUserFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.util.CompatibilityUtil;
import com.deathmotion.totemguard.common.util.ConsoleBanner;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import lombok.Setter;
import org.incendo.cloud.CommandManager;

import java.util.logging.Logger;

@Getter
public abstract class TGPlatform {

    @Getter
    private static final int bStatsId = 23179;
    @Getter
    private static TGPlatform instance;
    private final Platform platform;
    private final Logger logger;

    @Setter
    private boolean enabled = true;

    private ConfigRepositoryImpl configRepository;
    private EventRepositoryImpl eventRepository;
    private AlertManagerImpl alertManager;
    private PlayerRepositoryImpl playerRepository;
    private PlaceholderRepositoryImpl placeholderRepository;
    private CommandManagerImpl commandManager;

    private TGPlatformAPI api;

    public TGPlatform(Platform platform) {
        instance = this;
        this.platform = platform;
        logger = Logger.getLogger("TotemGuard");
    }

    public void commonOnInitialize() {
    }

    public void commonOnEnable() {
        if (!CompatibilityUtil.isCompatible()) {
            setEnabled(false);
            disablePlugin();
            return;
        }

        ConsoleBanner.print();

        configRepository = new ConfigRepositoryImpl();
        eventRepository = new EventRepositoryImpl();
        alertManager = new AlertManagerImpl();
        playerRepository = new PlayerRepositoryImpl();
        placeholderRepository = new PlaceholderRepositoryImpl();
        commandManager = new CommandManagerImpl();

        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerJoinQuit());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPingListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketCheckManagerListener());

        //noinspection resource
        eventRepository.subscribe(InventoryChangedEvent.class, new TotemReplenishedListener());
        //noinspection resource
        eventRepository.subscribeAllIncludingInternal(new EventCheckManagerListener());

        // Load the API
        api = new TGPlatformAPI();
        TotemGuard.init(api);

        enableBStats();
    }

    public void commonOnDisable() {
    }

    public abstract Scheduler getScheduler();

    public abstract CommandManager<Sender> getCommandManager();

    public abstract void enableBStats();

    public abstract PlatformUserFactory getPlatformUserFactory();

    public abstract String getPluginDirectory();

    public abstract String getPlatformVersion();

    public abstract boolean isPluginEnabled(String plugin);

    public abstract void disablePlugin();
}
