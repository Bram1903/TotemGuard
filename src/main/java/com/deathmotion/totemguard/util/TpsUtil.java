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

package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.TotemGuard;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TpsUtil {
    private static TpsUtil instance;
    private final boolean isFolia;

    private TpsUtil() {
        this.isFolia = FoliaScheduler.isFolia();
    }

    public static TpsUtil getInstance() {
        if (instance == null) {
            instance = new TpsUtil();
        }
        return instance;
    }

    public double getTps(Location location) {
        double rawTps;

        if (isFolia) {
            CompletableFuture<Double> future = new CompletableFuture<>();

            FoliaScheduler.getRegionScheduler().execute(
                    TotemGuard.getInstance(),
                    location,
                    () -> {
                        try {
                            future.complete(Bukkit.getTPS()[0]);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    }
            );

            try {
                rawTps = future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                rawTps = -1.0;
            }
        } else {
            rawTps = SpigotReflectionUtil.getTPS();
        }

        return Math.min(rawTps, 20.0);
    }

}
