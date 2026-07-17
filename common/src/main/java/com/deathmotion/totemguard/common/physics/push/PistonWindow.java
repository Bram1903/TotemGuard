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

package com.deathmotion.totemguard.common.physics.push;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.player.data.PistonData;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class PistonWindow {

    private final PistonData pistons;

    private double playerMinX, playerMinY, playerMinZ;
    private double playerMaxX, playerMaxY, playerMaxZ;

    private double pushLoX, pushHiX;
    @Getter
    private double pushLoY;
    private double pushHiY;
    private double pushLoZ, pushHiZ;
    @Getter
    private int launchMask;
    @Getter
    private boolean reaching;
    private boolean touching;

    public PistonWindow(PistonData pistons) {
        this.pistons = pistons;
    }

    public void setPlayerBox(double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ) {
        this.playerMinX = minX;
        this.playerMinY = minY;
        this.playerMinZ = minZ;
        this.playerMaxX = maxX;
        this.playerMaxY = maxY;
        this.playerMaxZ = maxZ;
    }

    public void evaluate() {
        pushLoX = 0.0;
        pushHiX = 0.0;
        pushLoY = 0.0;
        pushHiY = 0.0;
        pushLoZ = 0.0;
        pushHiZ = 0.0;
        launchMask = 0;
        reaching = false;
        touching = false;
        if (!pistons.isActive()) return;
        for (int i = 0; i < pistons.sceneCount(); i++) {
            PistonData.Scene scene = pistons.scene(i);
            if (!reaches(scene)) continue;
            reaching = true;
            if (overlaps(scene)) touching = true;
            pushLoX += scene.pushLoX();
            pushHiX += scene.pushHiX();
            pushLoY += scene.pushLoY();
            pushHiY += scene.pushHiY();
            pushLoZ += scene.pushLoZ();
            pushHiZ += scene.pushHiZ();
            launchMask |= scene.launchMask();
        }
        pushLoX = Math.max(pushLoX, -PistonData.PUSH_STEP);
        pushHiX = Math.min(pushHiX, PistonData.PUSH_STEP);
        pushLoY = Math.max(pushLoY, -PistonData.PUSH_STEP);
        pushHiY = Math.min(pushHiY, PistonData.PUSH_STEP);
        pushLoZ = Math.max(pushLoZ, -PistonData.PUSH_STEP);
        pushHiZ = Math.min(pushHiZ, PistonData.PUSH_STEP);
    }

    public double horizontalReach() {
        return Math.max(Math.max(-pushLoX, pushHiX), Math.max(-pushLoZ, pushHiZ));
    }

    public boolean apply(AreaBounds bounds) {
        if (!reaching) return false;
        if (touching) bounds.pistonReached(true);
        if (pushLoX != 0.0 || pushHiX != 0.0) bounds.extendPushX(pushLoX, pushHiX);
        if (pushLoZ != 0.0 || pushHiZ != 0.0) bounds.extendPushZ(pushLoZ, pushHiZ);
        if (pushHiY > 0.0) bounds.ceiling(bounds.ceiling() + pushHiY);
        if (pushLoY < 0.0) bounds.addDescentSlack(-pushLoY);
        return true;
    }

    private boolean overlaps(PistonData.Scene scene) {
        return playerMaxX > scene.minX() && playerMinX < scene.maxX() + 1.0
                && playerMaxY > scene.minY() && playerMinY < scene.maxY() + 1.0
                && playerMaxZ > scene.minZ() && playerMinZ < scene.maxZ() + 1.0;
    }

    private boolean reaches(PistonData.Scene scene) {
        return playerMaxX >= scene.minX() - PistonData.PUSH_STEP
                && playerMinX <= scene.maxX() + 1.0 + PistonData.PUSH_STEP
                && playerMaxY >= scene.minY() - PistonData.PUSH_STEP
                && playerMinY <= scene.maxY() + 1.0 + PistonData.PUSH_STEP
                && playerMaxZ >= scene.minZ() - PistonData.PUSH_STEP
                && playerMinZ <= scene.maxZ() + 1.0 + PistonData.PUSH_STEP;
    }
}
