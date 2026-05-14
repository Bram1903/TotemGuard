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

package com.deathmotion.totemguard.common.features.follow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight x/y/z store for active followers. Populated by
 * {@link FollowerPacketListener} off the packet thread and read by
 * {@link FollowRepository}'s tick on the async scheduler thread.
 *
 * <p>Uniform path for TG-tracked players and bypassed players — we never need
 * rotation or world here, the follow tick treats either as "good enough" for
 * a 100-block separation check.</p>
 */
public final class FollowerPositionTracker {

    private final ConcurrentHashMap<UUID, double[]> positions = new ConcurrentHashMap<>();

    public void update(@NotNull UUID uuid, double x, double y, double z) {
        positions.put(uuid, new double[]{x, y, z});
    }

    public double @Nullable [] get(@NotNull UUID uuid) {
        return positions.get(uuid);
    }

    public void remove(@NotNull UUID uuid) {
        positions.remove(uuid);
    }

    public void clear() {
        positions.clear();
    }
}
