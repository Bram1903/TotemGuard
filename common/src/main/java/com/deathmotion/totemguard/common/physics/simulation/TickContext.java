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

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;

public final class TickContext {

    public PhysicsPreset preset;
    public MediumSample sample;
    public ControlEnvelope input;
    public GroundFacts ground;
    public ContactReport contact;
    public MediumModel medium;
    public AreaBounds bounds;

    public void fill(PhysicsPreset preset, MediumSample sample, ControlEnvelope input,
                     GroundFacts ground, ContactReport contact, MediumModel medium, AreaBounds bounds) {
        this.preset = preset;
        this.sample = sample;
        this.input = input;
        this.ground = ground;
        this.contact = contact;
        this.medium = medium;
        this.bounds = bounds;
    }
}
