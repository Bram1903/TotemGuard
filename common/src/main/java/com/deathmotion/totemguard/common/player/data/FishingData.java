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

package com.deathmotion.totemguard.common.player.data;

import java.util.HashMap;
import java.util.Map;

public class FishingData {

    public static final int NO_ENTITY = -1;

    private final Map<Integer, Integer> hookOwners = new HashMap<>();
    private final Map<Integer, Integer> hookedTargets = new HashMap<>();

    public void onHookSpawn(int hookId, int ownerId) {
        hookOwners.put(hookId, ownerId);
    }

    public boolean isHook(int entityId) {
        return hookOwners.containsKey(entityId);
    }

    public int ownerOf(int hookId) {
        return hookOwners.getOrDefault(hookId, NO_ENTITY);
    }

    public void setHooked(int hookId, int hookedId) {
        if (!hookOwners.containsKey(hookId)) return;
        if (hookedId >= 0) {
            hookedTargets.put(hookId, hookedId);
        } else {
            hookedTargets.remove(hookId);
        }
    }

    public int hookedOf(int hookId) {
        return hookedTargets.getOrDefault(hookId, NO_ENTITY);
    }

    public void onRemove(int entityId) {
        hookOwners.remove(entityId);
        hookedTargets.remove(entityId);
    }

    public void reset() {
        hookOwners.clear();
        hookedTargets.clear();
    }
}
