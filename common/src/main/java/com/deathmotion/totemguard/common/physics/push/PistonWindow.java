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

public final class PistonWindow {

    private final PistonData pistons;

    private double playerMinX, playerMinY, playerMinZ;
    private double playerMaxX, playerMaxY, playerMaxZ;

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

    public boolean apply(AreaBounds bounds) {
        if (!pistons.isActive()) return false;
        boolean applied = false;
        double launchX = 0.0, launchZ = 0.0;
        double launchUp = 0.0, launchDown = 0.0;
        for (int i = 0; i < pistons.sceneCount(); i++) {
            PistonData.Scene scene = pistons.scene(i);
            if (!reaches(scene)) continue;
            applied = true;

            double px = PistonData.PUSH_STEP * scene.dirX();
            double pz = PistonData.PUSH_STEP * scene.dirZ();
            if (px != 0.0) bounds.centerX(bounds.centerX() + px);
            if (pz != 0.0) bounds.centerZ(bounds.centerZ() + pz);
            if (scene.dirY() > 0) bounds.ceiling(bounds.ceiling() + PistonData.PUSH_STEP);
            else if (scene.dirY() < 0) bounds.addDescentSlack(PistonData.PUSH_STEP);
            if (scene.honeyFront() && (px != 0.0 || pz != 0.0)) {
                bounds.expandRadius(PistonData.PUSH_STEP);
            }

            if (scene.slimeFront()) {
                launchX += PistonData.SLIME_LAUNCH * scene.dirX();
                launchZ += PistonData.SLIME_LAUNCH * scene.dirZ();
                if (scene.dirY() > 0) launchUp = PistonData.SLIME_LAUNCH;
                else if (scene.dirY() < 0) launchDown = -PistonData.SLIME_LAUNCH;
            }
        }
        if (!applied) return false;

        if ((launchX != 0.0 || launchZ != 0.0) && !bounds.hasAltCenter()) {
            bounds.altCenter(bounds.centerX() + launchX, bounds.centerZ() + launchZ);
        }
        if (launchUp > 0.0) bounds.ceiling(bounds.ceiling() + launchUp);
        if (launchDown < 0.0) bounds.addDescentSlack(-launchDown);
        return true;
    }

    private boolean reaches(PistonData.Scene scene) {
        double minX = Math.min(scene.minX(), scene.minX() + scene.dirX());
        double minY = Math.min(scene.minY(), scene.minY() + scene.dirY());
        double minZ = Math.min(scene.minZ(), scene.minZ() + scene.dirZ());
        double maxX = Math.max(scene.maxX(), scene.maxX() + scene.dirX()) + 1.0;
        double maxY = Math.max(scene.maxY(), scene.maxY() + scene.dirY()) + 1.0;
        double maxZ = Math.max(scene.maxZ(), scene.maxZ() + scene.dirZ()) + 1.0;
        return playerMaxX >= minX && playerMinX <= maxX
                && playerMaxY >= minY && playerMinY <= maxY
                && playerMaxZ >= minZ && playerMinZ <= maxZ;
    }
}
