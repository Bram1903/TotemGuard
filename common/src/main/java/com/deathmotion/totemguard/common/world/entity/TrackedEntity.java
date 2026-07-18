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

import com.deathmotion.totemguard.common.util.ClientMath;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class TrackedEntity {

    public static final int INTERPOLATION_STEPS = 3;
    private static final double BOOST_AMPLITUDE = 1.15F;

    private final int interpolationSteps;
    private final EntityType type;
    private final boolean pushable;
    private final boolean standable;
    private final double baseHalfWidth;
    private final double baseHeight;

    private double scale = 1.0;
    private int slimeSize = 1;
    private boolean positioned;

    private double movementSpeed = Double.NaN;
    private double jumpStrength = Double.NaN;
    private double gravity = Double.NaN;
    private double flyingSpeed = Double.NaN;
    private double stepHeight = Double.NaN;

    private boolean boosting;
    private int boostTicks;
    private int boostTotal;
    private int boostLagSlack;
    private boolean suffocating;
    private boolean saddleSeen;
    private boolean saddled;
    private boolean harnessed;
    private boolean staysStill;

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
    private boolean interpolatedLast;
    private boolean queuedForAdvance;

    TrackedEntity(EntityType type, int interpolationSteps) {
        this.interpolationSteps = interpolationSteps;
        this.type = type;
        this.pushable = EntityRoles.pushable(type);
        this.standable = EntityRoles.standable(type);
        this.baseHalfWidth = EntityDims.width(type) / 2.0;
        this.baseHeight = EntityDims.height(type);
    }

    void snapTo(double x, double y, double z) {
        targetX = renderX = prevRenderX = x;
        targetY = renderY = prevRenderY = y;
        targetZ = renderZ = prevRenderZ = z;
        interpSteps = 0;
        positioned = true;
    }

    void interpolateTo(double x, double y, double z) {
        targetX = x;
        targetY = y;
        targetZ = z;
        interpSteps = interpolationSteps;
        positioned = true;
    }

    void driveAuthoritative(double x, double y, double z) {
        prevRenderX = renderX;
        prevRenderY = renderY;
        prevRenderZ = renderZ;
        renderX = targetX = x;
        renderY = targetY = y;
        renderZ = targetZ = z;
        interpSteps = 0;
        interpolatedLast = false;
        positioned = true;
    }

    void addDelta(double dx, double dy, double dz) {
        targetX += dx;
        targetY += dy;
        targetZ += dz;
        interpSteps = interpolationSteps;
    }

    boolean advance() {
        prevRenderX = renderX;
        prevRenderY = renderY;
        prevRenderZ = renderZ;
        if (interpSteps > 0) {
            double t = 1.0 / interpSteps;
            renderX += (targetX - renderX) * t;
            renderY += (targetY - renderY) * t;
            renderZ += (targetZ - renderZ) * t;
            interpSteps--;
            interpolatedLast = true;
            return false;
        }
        interpolatedLast = false;
        return true;
    }

    public boolean interpolating() {
        return interpSteps > 0 || interpolatedLast;
    }

    void queuedForAdvance(boolean queued) {
        this.queuedForAdvance = queued;
    }

    void scale(double scale) {
        this.scale = Math.max(0.0625, Math.min(16.0, scale));
    }

    void movementSpeed(double value) {
        this.movementSpeed = value;
    }

    void jumpStrength(double value) {
        this.jumpStrength = value;
    }

    void gravity(double value) {
        this.gravity = value;
    }

    void flyingSpeed(double value) {
        this.flyingSpeed = value;
    }

    void stepHeight(double value) {
        this.stepHeight = value;
    }

    void startBoost(int total) {
        this.boosting = true;
        this.boostTicks = 0;
        this.boostTotal = Math.max(1, total);
        this.boostLagSlack = 0;
    }

    void suffocating(boolean value) {
        this.suffocating = value;
    }

    void saddled(boolean value) {
        this.saddleSeen = true;
        this.saddled = value;
    }

    void harnessed(boolean value) {
        this.harnessed = value;
    }

    void staysStill(boolean value) {
        this.staysStill = value;
    }

    public void tickBoost() {
        if (boosting && boostTicks++ > boostTotal) {
            boosting = false;
        }
    }

    public void addBoostLag() {
        if (boosting) boostLagSlack++;
    }

    public double boostFactorCeiling(boolean modernTrig) {
        if (!boosting) return 1.0;
        int from = Math.min(boostTicks, boostTotal + 1);
        int to = Math.min(boostTicks + boostLagSlack, boostTotal + 1);
        int lowPeak = Math.max(from, Math.min(to, boostTotal / 2));
        int highPeak = Math.max(from, Math.min(to, boostTotal / 2 + 1));
        double ceiling = Math.max(
                Math.max(boostCurve(from, modernTrig), boostCurve(to, modernTrig)),
                Math.max(boostCurve(lowPeak, modernTrig), boostCurve(highPeak, modernTrig)));
        return Math.max(1.0, ceiling);
    }

    private double boostCurve(int tick, boolean modernTrig) {
        float angle = (float) tick / boostTotal * (float) Math.PI;
        return 1.0 + BOOST_AMPLITUDE * ClientMath.sin(angle, modernTrig);
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public double stepHeight() {
        return stepHeight;
    }

    public double jumpStrength() {
        return jumpStrength;
    }

    public double gravity() {
        return gravity;
    }

    public double flyingSpeed() {
        return flyingSpeed;
    }

    void slimeSize(int size) {
        this.slimeSize = Math.max(1, Math.min(127, size));
    }

    public double halfWidth() {
        return baseHalfWidth * scale * slimeSize;
    }

    public double height() {
        return baseHeight * scale * slimeSize;
    }

    public double spanMinX() {
        return Math.min(Math.min(prevRenderX, renderX), targetX);
    }

    public double spanMaxX() {
        return Math.max(Math.max(prevRenderX, renderX), targetX);
    }

    public double spanMinY() {
        return Math.min(Math.min(prevRenderY, renderY), targetY);
    }

    public double spanMaxY() {
        return Math.max(Math.max(prevRenderY, renderY), targetY);
    }

    public double spanMinZ() {
        return Math.min(Math.min(prevRenderZ, renderZ), targetZ);
    }

    public double spanMaxZ() {
        return Math.max(Math.max(prevRenderZ, renderZ), targetZ);
    }
}
