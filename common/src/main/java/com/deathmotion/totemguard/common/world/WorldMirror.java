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

package com.deathmotion.totemguard.common.world;

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.deathmotion.totemguard.common.world.block.ClientStateMap;
import com.deathmotion.totemguard.common.world.block.DimensionProfile;
import com.deathmotion.totemguard.common.world.block.PendingBlocks;
import com.deathmotion.totemguard.common.world.border.BorderMirror;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class WorldMirror {

    private final BlockStore blocks = new BlockStore();
    private final PendingBlocks pending = new PendingBlocks();
    private final BlockReader reader;
    private final EntityTracker entities = new EntityTracker();
    private final BorderMirror border = new BorderMirror();
    private final WorldReadiness readiness = new WorldReadiness();
    private final DimensionProfile dimension = new DimensionProfile();

    public WorldMirror(ClientVersion clientVersion) {
        this.reader = new BlockReader(blocks, pending, ClientStateMap.forClient(clientVersion));
    }

    public void onJoin(String worldName, DimensionType dimensionType, int minY) {
        dimension.update(worldName, dimensionType, minY);
        blocks.minY(minY);
        clearWorldState();
    }

    public void onWorldSwap(String worldName, DimensionType dimensionType, int minY) {
        dimension.update(worldName, dimensionType, minY);
        blocks.minY(minY);
        clearWorldState();
    }

    public void onConfigurationStart() {
        dimension.reset();
        clearWorldState();
    }

    public boolean isWorldChange(WrapperPlayServerRespawn respawn) {
        return dimension.isWorldChange(respawn);
    }

    private void clearWorldState() {
        blocks.clear();
        pending.clear();
        entities.clear();
        border.reset();
        readiness.reset();
    }
}
