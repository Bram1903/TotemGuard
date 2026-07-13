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

package com.deathmotion.totemguard.common.physics.body;

import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.control.PlayerControlResolver;
import com.deathmotion.totemguard.common.physics.medium.GlideState;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.medium.MediumSelect;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public final class PlayerBody implements SimulationBody {

    private final Data data;
    private final EngineActor actor;
    private final VersionGates gates;
    private final PoseTracker pose = new PoseTracker();
    private final MediumSelect mediums = new MediumSelect();
    private final PlayerControlResolver control;

    private boolean glideMediumLastTick;

    public PlayerBody(Data data, EngineActor actor, VersionGates gates) {
        this.data = data;
        this.actor = actor;
        this.gates = gates;
        this.control = new PlayerControlResolver(data, actor, gates);
    }

    @Override
    public BodyKind kind() {
        return BodyKind.PLAYER;
    }

    @Override
    public double halfWidth() {
        if (data.isSleeping()) return MotionDefaults.SLEEPING_SIZE / 2.0;
        return data.getAttributeData().width() / 2.0;
    }

    @Override
    public double height() {
        return pose.height(data);
    }

    @Override
    public double stepHeight() {
        return data.getAttributeData().stepHeight();
    }

    @Override
    public ShapeQuery shapeQuery(double feetY, boolean deepFall) {
        return new ShapeQuery(feetY, data.isSneaking(), standsOnPowderSnow(), deepFall);
    }

    public double lastHeight() {
        return pose.lastHeight();
    }

    public PoseTracker pose() {
        return pose;
    }

    public MediumSelect mediums() {
        return mediums;
    }

    public PlayerControlResolver control() {
        return control;
    }

    public MediumModel medium(MediumSample sample, boolean silent) {
        GlideState glideState = data.isGliding()
                ? GlideState.FLAG
                : data.getGlideData().claimActive() ? GlideState.CLAIM : GlideState.NONE;
        MediumModel medium = mediums.select(sample, glideState, gates.glideForceExitOnClimbable(),
                data.isFlying() && data.isCanFly());
        boolean glideNow = medium == mediums.glide();
        if (glideNow) {
            mediums.glide().prepare(glideState == GlideState.CLAIM, silent, data.getFireworkData());
        } else if (glideMediumLastTick) {
            data.getGlideData().armExit();
        }
        glideMediumLastTick = glideNow;
        return medium;
    }

    private boolean standsOnPowderSnow() {
        ItemStack boots = actor.bootsItem();
        return boots != null && boots.getType() == ItemTypes.LEATHER_BOOTS;
    }
}
