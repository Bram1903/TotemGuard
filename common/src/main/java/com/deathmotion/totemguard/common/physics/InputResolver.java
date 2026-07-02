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

package com.deathmotion.totemguard.common.physics;

import com.deathmotion.totemguard.common.physics.sim.MovementInput;
import com.deathmotion.totemguard.common.physics.world.BlockEnvironment;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.EffectData;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.PlayerAttributeData;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;

final class InputResolver {

    private static final double RISE_EPS = 0.001;
    private static final double COYOTE_TAKEOFF_EPS = 0.05;
    private static final double WATER_EXIT_SUPPORT_REACH = 0.6;

    private static final int SNEAK_CONFIRM = 3;
    private static final double SNEAK_DIAGONAL_FACTOR = Math.sqrt(2.0);

    private static final double SPRINT_MIN_SPEED = 0.15;
    private static final double SPRINT_BACKWARD_COS = -0.6;
    private static final int SPRINT_TURN_TOLERANCE = 8;
    private static final double SPRINT_GROUND_SLIPPERINESS = 0.61;

    private final Data data;

    private int sneakStreak;
    private int backwardSprintStreak;
    private boolean improperSprint;

    InputResolver(Data data) {
        this.data = data;
    }

    boolean improperSprint() {
        return improperSprint;
    }

    MovementInput build(MovementData movement, BlockEnvironment env, Vector3d observed,
                        GroundState ground, double bubbleAscent) {
        InputData.State state = data.getInputData().current();
        boolean inventoryOpen = data.isOpenInventory();
        boolean horizontalInput = !inventoryOpen
                && (state == null || state.forward() || state.backward() || state.left() || state.right());
        boolean jumpHeld = state == null || state.jumping();

        EffectData effects = data.getEffectData();
        PlayerAttributeData attr = data.getAttributeData();
        int jumpBoostAmplifier = effects.hasJumpBoost() ? effects.jumpBoostAmplifier() : -1;
        double jumpTakeoff = attr.jumpStrength() + (jumpBoostAmplifier >= 0 ? 0.1 * (jumpBoostAmplifier + 1) : 0.0);

        boolean freshJump = observed.getY() >= jumpTakeoff - COYOTE_TAKEOFF_EPS;
        boolean coyoteJump = !ground.groundedStart() && ground.recentlyGrounded() && jumpHeld && freshJump
                && !inventoryOpen && !ground.coyoteBlocked();
        boolean effectiveGroundedStart = ground.groundedStart() || coyoteJump;
        boolean jumpPossible = effectiveGroundedStart && jumpHeld && !inventoryOpen;

        boolean sprinting = !inventoryOpen && data.isSprinting() && data.getFoodData().canSprint()
                && sprintForward(movement, env, state, observed, ground.groundedStart());
        improperSprint = data.isSprinting() && !sprinting && !inventoryOpen;

        boolean clippedJump = env.ceilingGap() < jumpTakeoff && observed.getY() > RISE_EPS;
        boolean sprintJump = effectiveGroundedStart && sprinting && (freshJump || clippedJump);

        sneakStreak = data.isSneaking() ? sneakStreak + 1 : 0;
        boolean sneaking = sneakStreak >= SNEAK_CONFIRM;
        boolean diagonal = state == null
                || ((state.forward() ^ state.backward()) && (state.left() ^ state.right()));

        boolean waterExitHop = ground.wasFluid() && !env.fluid() && observed.getY() > RISE_EPS
                && env.groundGap() <= WATER_EXIT_SUPPORT_REACH;

        return new MovementInput(effectiveGroundedStart, ground.groundedEnd(), horizontalInput, jumpPossible,
                sprinting, sprintJump, effectiveSpeed(sprinting, sneaking, diagonal),
                attr.jumpStrength(), attr.gravity(), attr.stepHeight(),
                ground.startSlipperinessMin(), ground.startSlipperinessMax(),
                effectiveBlockSpeedFactor(env),
                jumpBoostAmplifier,
                effects.hasLevitation(), effects.levitationAmplifier(), effects.hasSlowFalling(),
                fluidFriction(data.isSprinting(), effectiveGroundedStart, effects),
                fluidAccel(data.isSprinting(), effectiveGroundedStart),
                waterExitHop, bubbleAscent);
    }

    private double effectiveBlockSpeedFactor(BlockEnvironment env) {
        double raw = env.blockSpeedFactor();
        if (raw >= 1.0) return 1.0;
        if (!data.getPlayer().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) return 1.0;
        double efficiency = data.getAttributeData().movementEfficiency();
        return raw + efficiency * (1.0 - raw);
    }

    private double waterEfficiency(boolean groundedStart) {
        double wme = observesWaterEfficiency() ? data.getAttributeData().waterMovementEfficiency() : 1.0;
        return groundedStart ? wme : wme * 0.5;
    }

    private boolean observesWaterEfficiency() {
        return data.getPlayer().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21);
    }

    private double fluidFriction(boolean sprinting, boolean groundedStart, EffectData effects) {
        if (effects.hasDolphinsGrace()) return MovementConstants.WATER_DOLPHIN_FRICTION;
        double friction = sprinting ? MovementConstants.WATER_SPRINT_FRICTION : MovementConstants.WATER_FRICTION;
        if (observesWaterEfficiency()) {
            friction += (MovementConstants.WATER_EFFICIENCY_FRICTION_TARGET - friction) * waterEfficiency(groundedStart);
        }
        return friction;
    }

    private double fluidAccel(boolean sprinting, boolean groundedStart) {
        double getSpeed = data.getAttributeData().movementSpeed() * (sprinting ? MovementConstants.SPRINT_SPEED_MULTIPLIER : 1.0);
        double accel = MovementConstants.WATER_ACCEL + (getSpeed - MovementConstants.WATER_ACCEL) * waterEfficiency(groundedStart);
        return accel + MovementConstants.WATER_CURRENT_PUSH;
    }

    private boolean sprintForward(MovementData movement, BlockEnvironment env, InputData.State state, Vector3d observed,
                                  boolean groundedStart) {
        if (state != null) {
            backwardSprintStreak = 0;
            return state.forward();
        }
        double speed = Math.hypot(observed.getX(), observed.getZ());
        boolean onNormalGround = groundedStart && env.slipperinessMax() <= SPRINT_GROUND_SLIPPERINESS;
        boolean backward = false;
        if (onNormalGround && speed > SPRINT_MIN_SPEED) {
            double yaw = Math.toRadians(movement.getCurrent().getYaw());
            double forwardComponent = observed.getX() * -Math.sin(yaw) + observed.getZ() * Math.cos(yaw);
            backward = forwardComponent / speed < SPRINT_BACKWARD_COS;
        }
        backwardSprintStreak = backward ? backwardSprintStreak + 1 : 0;
        return backwardSprintStreak <= SPRINT_TURN_TOLERANCE;
    }

    private double effectiveSpeed(boolean sprinting, boolean sneaking, boolean diagonal) {
        double speed = data.getAttributeData().movementSpeed();
        if (sprinting) speed *= MovementConstants.SPRINT_SPEED_MULTIPLIER;
        if (sneaking) {
            double sneak = data.getAttributeData().sneakingSpeed();
            if (diagonal) sneak = Math.min(1.0, sneak * SNEAK_DIAGONAL_FACTOR);
            speed *= sneak;
        }
        return speed;
    }

    void suppressImproperSprint() {
        improperSprint = false;
    }

    void onDecline() {
        backwardSprintStreak = 0;
        improperSprint = false;
    }

    void clear() {
        sneakStreak = 0;
        backwardSprintStreak = 0;
        improperSprint = false;
    }
}
