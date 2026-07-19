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

package com.deathmotion.totemguard.common.physics.control;

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.VehicleData;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public final class RiderControlResolver {

    public static final double STEP_HEIGHT_HORSE = 1.0;
    public static final double STEP_HEIGHT_CAMEL = 1.5;

    private static final double HORSE_JUMP_LEAP = 0.4;
    private static final double CAMEL_SPRINT_BONUS = 0.1;
    private static final double CAMEL_DASH_HORIZONTAL = 22.2222;
    private static final double CAMEL_DASH_VERTICAL = 1.4285;
    private static final double PIG_SPEED_FACTOR = 0.225;
    private static final double STRIDER_SPEED_FACTOR = 0.55;
    private static final double STRIDER_SUFFOCATING_FACTOR = 0.35;
    private static final double STRIDER_SUFFOCATING_FACTOR_LEGACY = 0.23;

    private RiderControlResolver() {
    }

    public static RiderControl build(TrackedEntity ridden, EntityType type, VehicleData vehicle, Data data,
                                     VersionGates gates, float yaw, boolean grounded, boolean water,
                                     boolean controlling) {
        double speed = ridden.movementSpeed();
        boolean steerable = EntityRoles.steerableMob(type);
        boolean camel = EntityRoles.camel(type);
        boolean strider = type == EntityTypes.STRIDER;

        double steeringFactor = strider
                ? (ridden.suffocating()
                ? (gates.modernStriderSuffocation()
                ? STRIDER_SUFFOCATING_FACTOR : STRIDER_SUFFOCATING_FACTOR_LEGACY)
                : STRIDER_SPEED_FACTOR)
                : PIG_SPEED_FACTOR;
        double riddenSpeed = steerable
                ? speed * steeringFactor * ridden.boostFactorCeiling(gates.modernTrig())
                : speed + (camel && data.isSprinting() ? CAMEL_SPRINT_BONUS : 0.0);
        if (!controlling) riddenSpeed = 0.0;
        double gravity = Double.isNaN(ridden.gravity()) ? MotionDefaults.GRAVITY : ridden.gravity();
        double jumpStrength = ridden.jumpStrength();

        boolean jumpTick = !water && grounded && !steerable && vehicle.hasJumpClaim();
        double jumpTakeoff = 0.0;
        double leapRadius = 0.0;
        double dashVertical = 0.0;
        if (jumpTick) {
            vehicle.tickJumpClaimGrounded();
            jumpTick = controlling;
        }
        if (jumpTick) {
            if (!Double.isNaN(jumpStrength)) {
                double scale = vehicle.jumpClaimScale();
                if (camel) {
                    leapRadius = CAMEL_DASH_HORIZONTAL * scale * speed;
                    dashVertical = CAMEL_DASH_VERTICAL * scale * jumpStrength;
                } else {
                    leapRadius = HORSE_JUMP_LEAP * scale;
                    jumpTakeoff = jumpStrength * scale;
                }
            }
        }

        double defaultStep = camel ? STEP_HEIGHT_CAMEL : STEP_HEIGHT_HORSE;
        double stepHeight = Math.max(
                Double.isNaN(ridden.stepHeight()) ? defaultStep : ridden.stepHeight(),
                STEP_HEIGHT_HORSE);
        boolean canFloatInWater = EntityRoles.floatsWhileRidden(type) && gates.floatWhileRidden();
        boolean modernTrig = gates.modernTrig();
        return new RiderControl(riddenSpeed, gravity, stepHeight, jumpStrength, jumpTakeoff,
                ClientMath.lookX(yaw, 0.0f, modernTrig), ClientMath.lookZ(yaw, 0.0f, modernTrig),
                leapRadius, dashVertical, steerable, canFloatInWater, jumpTick);
    }
}
