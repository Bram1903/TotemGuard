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

package com.deathmotion.totemguard.fabric.tick;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.fabric.FabricServerHolder;
import com.deathmotion.totemguard.host.RuntimeDispatch;
import net.minecraft.server.MinecraftServer;

public final class FabricTickRunner {

    private final TGPlatform platform;

    public FabricTickRunner(TGPlatform platform) {
        this.platform = platform;
    }

    public void start() {
        RuntimeDispatch.setServerTickHandler(this::onServerTick);
    }

    public void stop() {
        RuntimeDispatch.clearServerTickHandler();
    }

    private void onServerTick() {
        if (!platform.isEnabled()) return;
        MinecraftServer server = FabricServerHolder.server();
        if (server != null) {
            FabricNetherPortalTracker.markNetherPortalContacts(platform, server);
        }
        platform.tickPlayers();
    }
}
