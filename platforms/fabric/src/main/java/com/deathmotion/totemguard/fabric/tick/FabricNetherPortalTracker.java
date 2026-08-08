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
import com.deathmotion.totemguard.common.player.TGPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

final class FabricNetherPortalTracker {

    private FabricNetherPortalTracker() {
    }

    static void markNetherPortalContacts(TGPlatform platform, MinecraftServer server) {
        for (TGPlayer tgPlayer : platform.getPlayerRepository().getPlayers()) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(tgPlayer.getUuid());
            if (serverPlayer != null && intersectsNetherPortal(serverPlayer)) {
                tgPlayer.getData().markNetherPortalContact();
            }
        }
    }

    private static boolean intersectsNetherPortal(ServerPlayer player) {
        Level level = player.level();
        AABB box = player.getBoundingBox();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
