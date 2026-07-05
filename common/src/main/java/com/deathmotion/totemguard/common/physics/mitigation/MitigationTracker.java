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

package com.deathmotion.totemguard.common.physics.mitigation;

import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.mitigation.MitigationService;
import com.deathmotion.totemguard.common.mitigation.SetbackController;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.verdict.MitigationOutcome;
import com.deathmotion.totemguard.common.player.data.Data;
import com.github.retrooper.packetevents.protocol.world.Location;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class MitigationTracker {

    private static final double BASE_GAIN = 1.0;
    private static final double EXCESS_GAIN = 12.0;
    private static final double DECAY_FACTOR = 0.92;
    private static final double DECAY_SUBTRACT = 0.30;

    private final Data data;
    private final MitigationService service;
    private final SetbackController controller;

    @Getter
    private double buffer;
    @Getter
    private boolean triggeredThisTick;
    @Getter
    private boolean setbackIssuedThisTick;
    @Getter
    private boolean setbackSkippedThisTick;
    @Getter
    private boolean inventoryClosedThisTick;

    public MitigationTracker(Data data) {
        this.data = data;
        this.service = data.getMitigationService();
        this.controller = data.getSetbackController();
    }

    public void observe(ConfigView view, PhysicsPreset preset,
                        boolean offense, double excess, boolean inventoryMove, boolean trustedPosition,
                        double safeVelY, double safeGroundGap, boolean safeAirborne) {
        clearTickFlags();

        controller.tickAnchorFreeze();
        service.onFlying();
        if (service.setbackConfirmedThisTick()) {
            buffer = 0.0;
        }

        if (!offense) {
            buffer = Math.max(0.0, buffer * DECAY_FACTOR - DECAY_SUBTRACT);
            if (trustedPosition && !service.setbackConfirmedThisTick()) {
                Location current = data.getMovementData().getCurrent();
                controller.markSafe(current.getX(), current.getY(), current.getZ(),
                        safeVelY, safeGroundGap, safeAirborne);
            }
            return;
        }

        double threshold = preset.setbackBufferThreshold();
        buffer += BASE_GAIN + excess * EXCESS_GAIN;
        if (buffer < threshold) return;

        boolean setbackWanted = view.physicsEngineSetback();
        if (setbackWanted && service.setbackPending()) {
            buffer = threshold;
            return;
        }

        buffer = 0.0;
        triggeredThisTick = true;

        if (inventoryMove && view.physicsEngineCloseInventory()) {
            service.closeInventory();
            inventoryClosedThisTick = true;
        }
        setback(setbackWanted);
    }

    public MitigationOutcome outcome() {
        if (!triggeredThisTick && !setbackIssuedThisTick && !setbackSkippedThisTick && !inventoryClosedThisTick) {
            return MitigationOutcome.NONE;
        }
        return new MitigationOutcome(triggeredThisTick, setbackIssuedThisTick,
                setbackSkippedThisTick, inventoryClosedThisTick);
    }

    public void clearTickFlags() {
        triggeredThisTick = false;
        setbackIssuedThisTick = false;
        setbackSkippedThisTick = false;
        inventoryClosedThisTick = false;
    }

    public void reset() {
        buffer = 0.0;
        clearTickFlags();
        service.reset();
        controller.reset();
    }

    private void setback(boolean issue) {
        if (service.setbackPending() || !controller.hasSafe() || !service.setbackIssuable()) return;
        if (!issue) {
            setbackSkippedThisTick = true;
            return;
        }
        setbackIssuedThisTick = controller.requestSetback();
    }
}
