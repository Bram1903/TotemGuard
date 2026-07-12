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

public class ExternalVelocityData {

    private static final int WINDOW_TICKS = 3;

    private double setX;
    private double setY;
    private double setZ;
    private boolean hasSet;
    private double addX;
    private double addY;
    private double addZ;
    private double slack;
    private int ticks;
    private boolean active;
    private int setSequence;
    private boolean expiredSet;

    public void setVelocity(double vx, double vy, double vz) {
        setX = vx;
        setY = vy;
        setZ = vz;
        hasSet = true;
        addX = 0.0;
        addY = 0.0;
        addZ = 0.0;
        slack = 0.0;
        active = true;
        ticks = WINDOW_TICKS;
        setSequence++;
    }

    public void addPush(double px, double py, double pz) {
        addPush(px, py, pz, 0.0);
    }

    public void addPush(double px, double py, double pz, double pushSlack) {
        if (!active) {
            addX = 0.0;
            addY = 0.0;
            addZ = 0.0;
            slack = 0.0;
            hasSet = false;
        }
        addX += px;
        addY += py;
        addZ += pz;
        slack += pushSlack;
        active = true;
        ticks = WINDOW_TICKS;
    }

    public void tick() {
        if (!active) return;
        if (--ticks <= 0) {
            if (hasSet) expiredSet = true;
            clear();
        }
    }

    public void consume() {
        clear();
    }

    public boolean pollExpiredSet() {
        boolean expired = expiredSet;
        expiredSet = false;
        return expired;
    }

    public double x() {
        return hasSet ? setX + addX : addX;
    }

    public double y() {
        return hasSet ? setY + addY : addY;
    }

    public double z() {
        return hasSet ? setZ + addZ : addZ;
    }

    public double slack() {
        return slack;
    }

    public boolean hasSet() {
        return hasSet;
    }

    public int setSequence() {
        return setSequence;
    }

    public boolean isActive() {
        return active;
    }

    public void reset() {
        clear();
        expiredSet = false;
    }

    private void clear() {
        setX = 0.0;
        setY = 0.0;
        setZ = 0.0;
        hasSet = false;
        addX = 0.0;
        addY = 0.0;
        addZ = 0.0;
        slack = 0.0;
        ticks = 0;
        active = false;
    }
}
