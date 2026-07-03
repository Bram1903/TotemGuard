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

package com.deathmotion.totemguard.common.world.entity;

import com.deathmotion.totemguard.common.world.collisions.EntityHitboxes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.Getter;

@Getter
public final class TrackedEntity {

    private static final int INTERPOLATION_STEPS = 3;

    private final EntityType type;
    private final boolean pushable;
    private final boolean standable;
    private final double baseHalfWidth;
    private final double baseHeight;

    private double scale = 1.0;
    private int slimeSize = 1;
    private boolean positioned;

    private double targetX;
    private double targetY;
    private double targetZ;
    private double renderX;
    private double renderY;
    private double renderZ;
    private double prevRenderX;
    private double prevRenderY;
    private double prevRenderZ;
    private int interpSteps;

    public TrackedEntity(EntityType type) {
        this.type = type;
        this.pushable = EntityCollisionTypes.isPushable(type);
        this.standable = EntityCollisionTypes.isStandable(type);
        this.baseHalfWidth = EntityHitboxes.width(type) / 2.0;
        this.baseHeight = EntityHitboxes.height(type);
    }

    public void snapTo(double x, double y, double z) {
        targetX = renderX = prevRenderX = x;
        targetY = renderY = prevRenderY = y;
        targetZ = renderZ = prevRenderZ = z;
        interpSteps = 0;
        positioned = true;
    }

    public void interpolateTo(double x, double y, double z) {
        targetX = x;
        targetY = y;
        targetZ = z;
        interpSteps = INTERPOLATION_STEPS;
        positioned = true;
    }

    public void addDelta(double dx, double dy, double dz) {
        targetX += dx;
        targetY += dy;
        targetZ += dz;
        interpSteps = INTERPOLATION_STEPS;
    }

    public void advance() {
        prevRenderX = renderX;
        prevRenderY = renderY;
        prevRenderZ = renderZ;
        if (interpSteps > 0) {
            double t = 1.0 / interpSteps;
            renderX += (targetX - renderX) * t;
            renderY += (targetY - renderY) * t;
            renderZ += (targetZ - renderZ) * t;
            interpSteps--;
        }
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.0625, Math.min(16.0, scale));
    }

    public void setSlimeSize(int size) {
        this.slimeSize = Math.max(1, Math.min(127, size));
    }

    public double halfWidth() {
        return baseHalfWidth * scale * slimeSize;
    }

    public double height() {
        return baseHeight * scale * slimeSize;
    }
}
