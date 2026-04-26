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

package com.deathmotion.totemguard.fabric;

import lombok.Getter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class TGFabric implements DedicatedServerModInitializer {

    @Getter
    private static TGFabric instance;

    private TGFabricPlatform tg;

    @Override
    public void onInitializeServer() {
        instance = this;
        tg = new TGFabricPlatform(this, FabricLoader.getInstance().getConfigDir().resolve("totemguard"));
        tg.commonOnInitialize();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> tg.commonOnEnable());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> tg.commonOnDisable());
    }

    public TGFabricPlatform getTg() {
        return tg;
    }
}
