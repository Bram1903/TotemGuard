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

package com.deathmotion.totemguard.common.physics.medium;

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;

public final class FlowSolver {

    public static final double MIN_PUSH = 0.0045000000000000005;
    private static final double MIN_PUSH_GATE = 0.003;

    private static final double BOX_DEFLATE = 0.001;
    private static final double WATER_SCALE = 0.014;
    private static final double LAVA_SCALE_ULTRAWARM = 0.007;
    private static final double LAVA_SCALE = 0.0023333333333333335;
    private static final float SOURCE_DROP = 0.8888889F;
    private static final double NORMALIZE_EPS = 1.0E-4;
    private static final double SHALLOW_SCALE_LIMIT = 0.4;

    private FlowSolver() {
    }

    public static void solve(BlockReader reader, MediumSample out, boolean lavaFast,
                             double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ) {
        double boxMinX = minX + BOX_DEFLATE;
        double boxMinY = minY + BOX_DEFLATE;
        double boxMinZ = minZ + BOX_DEFLATE;
        double boxMaxX = maxX - BOX_DEFLATE;
        double boxMaxY = maxY - BOX_DEFLATE;
        double boxMaxZ = maxZ - BOX_DEFLATE;
        int x0 = floor(boxMinX), x1 = ceil(boxMaxX) - 1;
        int y0 = floor(boxMinY), y1 = ceil(boxMaxY) - 1;
        int z0 = floor(boxMinZ), z1 = ceil(boxMaxZ) - 1;

        double waterHeight = 0.0, waterX = 0.0, waterY = 0.0, waterZ = 0.0;
        double lavaHeight = 0.0, lavaX = 0.0, lavaY = 0.0, lavaZ = 0.0;
        int waterCount = 0, lavaCount = 0;
        double flowX, flowY, flowZ;

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    long facts = reader.facts(x, y, z);
                    if (!StateFacts.is(facts, StateFacts.ANY_FLUID)) continue;
                    boolean lava = StateFacts.is(facts, StateFacts.LAVA);
                    long kind = lava ? StateFacts.LAVA : StateFacts.WATER;
                    double height = StateFacts.is(reader.facts(x, y + 1, z), kind)
                            ? 1.0
                            : StateFacts.fluidAmount(facts) / 9.0F;
                    double surface = y + height;
                    if (surface < boxMinY) continue;

                    double running = surface - boxMinY;
                    {
                        float ownHeight = StateFacts.fluidAmount(facts) / 9.0F;
                        double fx = 0.0, fz = 0.0;
                        for (int dir = 0; dir < 4; dir++) {
                            int stepX = STEP_X[dir], stepZ = STEP_Z[dir];
                            long nf = reader.facts(x + stepX, y, z + stepZ);
                            boolean sameFluid = StateFacts.is(nf, kind);
                            if (!sameFluid && StateFacts.is(nf, StateFacts.ANY_FLUID)) continue;
                            float f = sameFluid ? StateFacts.fluidAmount(nf) / 9.0F : 0.0F;
                            float f1 = 0.0F;
                            if (f == 0.0F) {
                                if (!StateFacts.is(nf, StateFacts.BLOCKS_MOTION)) {
                                    long bf = reader.facts(x + stepX, y - 1, z + stepZ);
                                    if (StateFacts.is(bf, kind)) {
                                        float below = StateFacts.fluidAmount(bf) / 9.0F;
                                        if (below > 0.0F) f1 = ownHeight - (below - SOURCE_DROP);
                                    }
                                }
                            } else {
                                f1 = ownHeight - f;
                            }
                            if (f1 != 0.0F) {
                                fx += stepX * f1;
                                fz += stepZ * f1;
                            }
                        }
                        double fy = 0.0;
                        if (StateFacts.is(facts, StateFacts.FLUID_FALLING)) {
                            for (int dir = 0; dir < 4; dir++) {
                                int nx = x + STEP_X[dir], nz = z + STEP_Z[dir];
                                if (solidFace(reader.facts(nx, y, nz), kind)
                                        || solidFace(reader.facts(nx, y + 1, nz), kind)) {
                                    double len = Math.sqrt(fx * fx + fz * fz);
                                    if (len >= NORMALIZE_EPS) {
                                        fx /= len;
                                        fz /= len;
                                    } else {
                                        fx = 0.0;
                                        fz = 0.0;
                                    }
                                    fy = -6.0;
                                    break;
                                }
                            }
                        }
                        double len = Math.sqrt(fx * fx + fy * fy + fz * fz);
                        if (len >= NORMALIZE_EPS) {
                            flowX = fx / len;
                            flowY = fy / len;
                            flowZ = fz / len;
                        } else {
                            flowX = 0.0;
                            flowY = 0.0;
                            flowZ = 0.0;
                        }
                    }

                    if (lava) {
                        lavaHeight = Math.max(running, lavaHeight);
                        if (lavaHeight < SHALLOW_SCALE_LIMIT) {
                            flowX *= lavaHeight;
                            flowY *= lavaHeight;
                            flowZ *= lavaHeight;
                        }
                        lavaX += flowX;
                        lavaY += flowY;
                        lavaZ += flowZ;
                        lavaCount++;
                    } else {
                        waterHeight = Math.max(running, waterHeight);
                        if (waterHeight < SHALLOW_SCALE_LIMIT) {
                            flowX *= waterHeight;
                            flowY *= waterHeight;
                            flowZ *= waterHeight;
                        }
                        waterX += flowX;
                        waterY += flowY;
                        waterZ += flowZ;
                        waterCount++;
                    }
                }
            }
        }

        double pushX = 0.0, pushY = 0.0, pushZ = 0.0;
        if (waterCount > 0 && lengthSquared(waterX, waterY, waterZ) >= 1.0E-5F) {
            double inv = WATER_SCALE / waterCount;
            pushX += waterX * inv;
            pushY += waterY * inv;
            pushZ += waterZ * inv;
        }
        if (lavaCount > 0 && lengthSquared(lavaX, lavaY, lavaZ) >= 1.0E-5F) {
            double scale = lavaFast ? LAVA_SCALE_ULTRAWARM : LAVA_SCALE;
            double inv = scale / lavaCount;
            pushX += lavaX * inv;
            pushY += lavaY * inv;
            pushZ += lavaZ * inv;
        }
        out.pushX(pushX);
        out.pushY(pushY);
        out.pushZ(pushZ);
    }

    public static double minKickScale(double pushLength, double centerX, double centerZ) {
        if (pushLength <= 0.0 || pushLength >= MIN_PUSH) return 1.0;
        if (Math.abs(centerX) < MIN_PUSH_GATE && Math.abs(centerZ) < MIN_PUSH_GATE) {
            return MIN_PUSH / pushLength;
        }
        return 1.0;
    }

    private static final int[] STEP_X = {0, 0, -1, 1};
    private static final int[] STEP_Z = {-1, 1, 0, 0};

    private static boolean solidFace(long facts, long fluidKind) {
        return !StateFacts.is(facts, fluidKind) && StateFacts.is(facts, StateFacts.FLOW_STURDY);
    }

    private static double lengthSquared(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }
}
