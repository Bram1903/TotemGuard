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

package com.deathmotion.totemguard.common.physics.collision;

import com.deathmotion.totemguard.common.world.shape.ShapeSink;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class ColliderBuffer implements ShapeSink {

    public static final int STRIDE = 6;

    public static final long TAG_UNCERTAIN = 1L << 58;
    public static final long TAG_EXEMPT = 1L << 57;
    public static final long NO_CELL = Long.MIN_VALUE;
    private static final int KIND_SHIFT = 60;
    public static final long KIND_BLOCK = 0L << KIND_SHIFT;
    public static final long KIND_ENTITY = 1L << KIND_SHIFT;
    private static final long KIND_MASK = 3L << KIND_SHIFT;
    private double[] boxes = new double[STRIDE * 64];
    private long[] tags = new long[64];
    private long[] cells = new long[64];

    @Getter
    private int count;

    private long currentTag;
    private long currentCell = NO_CELL;

    public static boolean isBlock(long tag) {
        return (tag & KIND_MASK) == KIND_BLOCK;
    }

    public static boolean isEntity(long tag) {
        return (tag & KIND_MASK) == KIND_ENTITY;
    }

    public static boolean clipEligible(long tag) {
        return (tag & (TAG_UNCERTAIN | TAG_EXEMPT)) == 0;
    }

    public static long packCell(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    public static int cellX(long cellKey) {
        return (int) (cellKey >> 38);
    }

    public static int cellZ(long cellKey) {
        return (int) (cellKey << 26 >> 38);
    }

    public static int cellY(long cellKey) {
        return (int) (cellKey << 52 >> 52);
    }

    public void reset() {
        count = 0;
        currentTag = 0L;
        currentCell = NO_CELL;
    }

    public void tag(long tag) {
        this.currentTag = tag;
    }

    public void cell(long cellKey) {
        this.currentCell = cellKey;
    }

    @Override
    public void accept(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int base = count * STRIDE;
        if (base + STRIDE > boxes.length) {
            grow();
        }
        boxes[base] = minX;
        boxes[base + 1] = minY;
        boxes[base + 2] = minZ;
        boxes[base + 3] = maxX;
        boxes[base + 4] = maxY;
        boxes[base + 5] = maxZ;
        tags[count] = currentTag;
        cells[count] = currentCell;
        count++;
    }

    public double minX(int i) {
        return boxes[i * STRIDE];
    }

    public double minY(int i) {
        return boxes[i * STRIDE + 1];
    }

    public double minZ(int i) {
        return boxes[i * STRIDE + 2];
    }

    public double maxX(int i) {
        return boxes[i * STRIDE + 3];
    }

    public double maxY(int i) {
        return boxes[i * STRIDE + 4];
    }

    public double maxZ(int i) {
        return boxes[i * STRIDE + 5];
    }

    public long tagOf(int i) {
        return tags[i];
    }

    public long cellOf(int i) {
        return cells[i];
    }

    private void grow() {
        double[] newBoxes = new double[boxes.length * 2];
        long[] newTags = new long[tags.length * 2];
        long[] newCells = new long[cells.length * 2];
        System.arraycopy(boxes, 0, newBoxes, 0, count * STRIDE);
        System.arraycopy(tags, 0, newTags, 0, count);
        System.arraycopy(cells, 0, newCells, 0, count);
        boxes = newBoxes;
        tags = newTags;
        cells = newCells;
    }
}
