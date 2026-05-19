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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class FabricServerHolder {

    private static volatile @Nullable MinecraftServer instance;

    static {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> instance = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> instance = null);
    }

    private FabricServerHolder() {
    }

    // Forces class initialization at a known point so the static block registers its
    // lifecycle listeners before SERVER_STARTED fires. Without an explicit touch, the
    // class would not load until something on the runtime hot path referenced it, which
    // is after the event we want to observe.
    public static void init() {
    }

    public static @Nullable MinecraftServer server() {
        return instance;
    }
}
