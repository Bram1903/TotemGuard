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

package com.deathmotion.totemguard.common;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.packet.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.common.manager.PlayerManager;
import com.deathmotion.totemguard.common.platform.player.PlatformUserFactory;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;

import java.util.logging.Logger;

@Getter
public abstract class TGPlatform {

    @Getter
    private static TGPlatform instance;

    private final boolean isProxy;
    private final Logger logger;

    private EventRepositoryImpl eventRepository;
    private PlayerManager playerManager;

    private TGPlatformAPI api;

    public TGPlatform() {
        this(false);
    }

    public TGPlatform(boolean isProxy) {
        instance = this;
        this.isProxy = isProxy;
        logger = Logger.getLogger("TotemGuard");
    }

    public void commonOnInitialize() {
    }

    public void commonOnEnable() {
        eventRepository = new EventRepositoryImpl();
        playerManager = new PlayerManager();

        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerJoinQuit());

        api = new TGPlatformAPI();
        TotemGuard.init(api);
        logger.info("TotemGuard enabled.");
    }

    public void commonOnDisable() {
        logger.info("TotemGuard disabled.");
    }

    public abstract PlatformUserFactory getPlatformUserFactory();
}
