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

package com.deathmotion.totemguard.common.physics.simulation;

import com.deathmotion.totemguard.common.physics.area.CarriedHypotheses;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import org.jetbrains.annotations.Nullable;

public final class TickState {

    public PhysicsPreset preset;
    public MediumSample sample;
    public ContactReport contact;

    public double dx, dy, dz;
    public double observedSpeed;
    public @Nullable GroundFacts ground;
    public @Nullable ControlEnvelope input;
    public @Nullable MediumModel medium;

    public boolean scanned;
    public boolean doubleMove;
    public boolean previousClaimedGround;
    public boolean landMedium;
    public boolean honeySlide;
    public boolean stepped;
    public boolean stepFromFall;
    public boolean deltaZeroedDisplacement;

    public double frictionMax;
    public double speedFactor;

    public double preCarriedX, preCarriedZ, preCarriedFloor, preCarriedCeil;

    public double quietOwedRise;
    public double quietOwedFall;

    public void reset(PhysicsPreset preset, MediumSample sample, ContactReport contact,
                      double dx, double dy, double dz, double observedSpeed,
                      boolean previousClaimedGround) {
        this.preset = preset;
        this.sample = sample;
        this.contact = contact;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.observedSpeed = observedSpeed;
        this.previousClaimedGround = previousClaimedGround;
        this.ground = null;
        this.input = null;
        this.medium = null;
        this.scanned = false;
        this.doubleMove = false;
        this.landMedium = false;
        this.honeySlide = false;
        this.stepped = false;
        this.stepFromFall = false;
        this.deltaZeroedDisplacement = false;
        this.frictionMax = 0.0;
        this.speedFactor = 1.0;
        this.quietOwedRise = 0.0;
        this.quietOwedFall = 0.0;
    }

    public boolean landModel() {
        return medium != null && medium.kind() == MediumKind.LAND;
    }

    public boolean airRegime(CarriedHypotheses.Kind kind) {
        return kind == CarriedHypotheses.Kind.AIR_REGIME && !previousClaimedGround && landModel();
    }
}
