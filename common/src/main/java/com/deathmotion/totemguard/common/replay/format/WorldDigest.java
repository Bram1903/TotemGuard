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

package com.deathmotion.totemguard.common.replay.format;

import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record WorldDigest(
        int centerChunkX,
        int centerChunkZ,
        int radius,
        int baseY,
        int height,
        int columnsLoaded,
        long blockHash,
        int entityCount,
        long entityHash,
        long borderHash
) {
    public static final WorldDigest ABSENT =
            new WorldDigest(0, 0, 0, 0, 0, -1, 0L, -1, 0L, 0L);
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    public static WorldDigest of(WorldMirror mirror, double x, double y, double z, int radius, int height) {
        int centerChunkX = (int) Math.floor(x) >> 4;
        int centerChunkZ = (int) Math.floor(z) >> 4;
        int baseY = (int) Math.floor(y) - height / 2;
        return sample(mirror, centerChunkX, centerChunkZ, radius, baseY, height);
    }

    public static WorldDigest sample(WorldMirror mirror, int centerChunkX, int centerChunkZ,
                                     int radius, int baseY, int height) {
        return sample(mirror, centerChunkX, centerChunkZ, radius, baseY, height,
                (x, y, z) -> mirror.blocks().serverStateId(x, y, z),
                (chunkX, chunkZ) -> mirror.blocks().isLoaded(chunkX, chunkZ));
    }

    public static WorldDigest sample(WorldMirror mirror, int centerChunkX, int centerChunkZ,
                                     int radius, int baseY, int height,
                                     BlockPeek peek, ColumnPeek loaded) {
        long blockHash = FNV_OFFSET;
        int columnsLoaded = 0;
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                if (!loaded.isLoaded(chunkX, chunkZ)) continue;
                columnsLoaded++;
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        for (int offsetY = 0; offsetY < height; offsetY++) {
                            blockHash = mix(blockHash, peek.stateId(
                                    (chunkX << 4) + localX, baseY + offsetY, (chunkZ << 4) + localZ));
                        }
                    }
                }
            }
        }

        int entityCount = 0;
        long entityHash = FNV_OFFSET;
        for (TrackedEntity entity : mirror.entities().tracked()) {
            entityCount++;
            long perEntity = entity.uuidString() == null ? 0L : entity.uuidString().hashCode();
            perEntity = perEntity * FNV_PRIME + Double.doubleToRawLongBits(entity.halfWidth());
            perEntity = perEntity * FNV_PRIME + Double.doubleToRawLongBits(entity.height());
            perEntity = perEntity * FNV_PRIME + (entity.pushable() ? 0x9E3779B9L : 0L);
            perEntity = perEntity * FNV_PRIME + (entity.standable() ? 0x7F4A7C15L : 0L);
            entityHash ^= perEntity;
        }

        long borderHash = FNV_OFFSET;
        borderHash = mix(borderHash, Double.hashCode(mirror.border().minX()));
        borderHash = mix(borderHash, Double.hashCode(mirror.border().maxX()));
        borderHash = mix(borderHash, Double.hashCode(mirror.border().minZ()));
        borderHash = mix(borderHash, Double.hashCode(mirror.border().maxZ()));

        return new WorldDigest(centerChunkX, centerChunkZ, radius, baseY, height,
                columnsLoaded, blockHash, entityCount, entityHash, borderHash);
    }

    private static long mix(long hash, int value) {
        return (hash ^ (value & 0xFFFFFFFFL)) * FNV_PRIME;
    }

    public static WorldDigest read(DataInputStream in) throws IOException {
        return new WorldDigest(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readInt(), in.readLong(), in.readInt(), in.readLong(), in.readLong());
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(centerChunkX);
        out.writeInt(centerChunkZ);
        out.writeInt(radius);
        out.writeInt(baseY);
        out.writeInt(height);
        out.writeInt(columnsLoaded);
        out.writeLong(blockHash);
        out.writeInt(entityCount);
        out.writeLong(entityHash);
        out.writeLong(borderHash);
    }

    public boolean present() {
        return columnsLoaded >= 0;
    }

    public WorldDigest resample(WorldMirror mirror) {
        return sample(mirror, centerChunkX, centerChunkZ, radius, baseY, height);
    }

    public @Nullable String firstDifference(WorldDigest other) {
        if (columnsLoaded != other.columnsLoaded) {
            return "columnsLoaded " + columnsLoaded + " vs " + other.columnsLoaded
                    + " sampling chunk " + centerChunkX + "," + centerChunkZ
                    + " radius " + radius + " from y" + baseY;
        }
        if (blockHash != other.blockHash) return "blockHash";
        if (entityCount < 0 || other.entityCount < 0) return null;
        if (entityCount != other.entityCount) {
            return "entityCount " + entityCount + " vs " + other.entityCount;
        }
        if (entityHash != other.entityHash) return "entityHash";
        if (borderHash != other.borderHash) return "borderHash";
        return null;
    }

    @FunctionalInterface
    public interface BlockPeek {

        int stateId(int x, int y, int z);
    }

    @FunctionalInterface
    public interface ColumnPeek {

        boolean isLoaded(int chunkX, int chunkZ);
    }
}
