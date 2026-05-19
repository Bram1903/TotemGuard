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

import java.lang.reflect.Method;

public final class FabricServerHolder {

    private static volatile @Nullable MinecraftServer instance;
    private static volatile @Nullable Method loaderHolderServerMethod;
    private static volatile boolean loaderHolderResolved;

    static {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> instance = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> instance = null);
    }

    private FabricServerHolder() {
    }

    public static void init() {
    }

    public static @Nullable MinecraftServer server() {
        MinecraftServer own = instance;
        if (own != null) return own;
        return serverFromLoaderHolder();
    }

    private static @Nullable MinecraftServer serverFromLoaderHolder() {
        Method method = loaderHolderServerMethod;
        if (method == null) {
            if (loaderHolderResolved) return null;
            synchronized (FabricServerHolder.class) {
                if (loaderHolderResolved) {
                    method = loaderHolderServerMethod;
                } else {
                    try {
                        Class<?> holder = Class.forName(
                                "com.deathmotion.totemguard.loader.fabric.LoaderServerHolder",
                                true,
                                FabricServerHolder.class.getClassLoader());
                        method = holder.getMethod("server");
                        loaderHolderServerMethod = method;
                    } catch (Throwable ignored) {
                        // Standalone: no loader holder on the classpath.
                    }
                    loaderHolderResolved = true;
                }
            }
        }
        if (method == null) return null;
        try {
            return (MinecraftServer) method.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
