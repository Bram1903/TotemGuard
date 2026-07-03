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
import com.deathmotion.totemguard.common.physics.MovementCause;
import com.deathmotion.totemguard.common.physics.MovementDebug;
import com.deathmotion.totemguard.common.physics.MovementResult;
import com.deathmotion.totemguard.common.player.data.Data;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class MovementMitigation {

    private static final double BASE_GAIN = 1.0;
    private static final double EXCESS_GAIN = 12.0;
    private static final double DECAY_FACTOR = 0.92;
    private static final double DECAY_SUBTRACT = 0.30;

    private final Data data;
    private final MitigationService service;

    private Vector3d lastLegal;
    private double buffer;
    @Getter
    private boolean triggeredThisTick;
    @Getter
    private boolean setbackIssuedThisTick;
    @Getter
    private boolean setbackSkippedThisTick;

    public MovementMitigation(Data data) {
        this.data = data;
        this.service = data.getMitigationService();
    }

    public void observe(MovementResult result, ConfigView view) {
        triggeredThisTick = false;
        setbackIssuedThisTick = false;
        setbackSkippedThisTick = false;

        service.onFlying();
        if (service.setbackConfirmedThisTick()) buffer = 0.0;

        boolean offense = result.movedThisTick() || result.ascendingThisTick();
        if (!offense) {
            buffer = Math.max(0.0, buffer * DECAY_FACTOR - DECAY_SUBTRACT);
            if (trustedPosition(result.cause())) {
                Location current = data.getMovementData().getCurrent();
                lastLegal = new Vector3d(current.getX(), current.getY(), current.getZ());
            }
            return;
        }

        double threshold = view.physicsEngineTolerance().setbackBuffer();
        double excess = Math.max(result.horizontalExcess(), result.verticalExcess());
        buffer += BASE_GAIN + excess * EXCESS_GAIN;
        if (buffer < threshold) return;

        boolean setbackWanted = view.physicsEngineSetback();
        if (setbackWanted && service.setbackPending()) {
            buffer = threshold;
            return;
        }

        buffer = 0.0;
        triggeredThisTick = true;

        if (result.cause() == MovementCause.INVENTORY_MOVE && view.physicsEngineCloseInventory()) {
            service.closeInventory();
        }
        setback(result, setbackWanted);
    }

    public void clearTickFlags() {
        triggeredThisTick = false;
        setbackIssuedThisTick = false;
        setbackSkippedThisTick = false;
    }

    public void reset() {
        lastLegal = null;
        buffer = 0.0;
        triggeredThisTick = false;
        setbackIssuedThisTick = false;
        setbackSkippedThisTick = false;
        service.reset();
    }

    private boolean trustedPosition(MovementCause cause) {
        return cause != MovementCause.WITHHELD
                && cause != MovementCause.DOUBLE_MOVE
                && cause != MovementCause.TELEPORT;
    }

    private void setback(MovementResult result, boolean issue) {
        if (service.setbackPending() || lastLegal == null) return;
        if (!service.setbackIssuable()) return;

        if (!issue) {
            setbackSkippedThisTick = true;
            return;
        }

        MovementDebug.log(data.getPlayer(), "setback:" + result.cause(), result.observed(),
                null, null, null, result.horizontalExcess(), result.verticalExcess());
        setbackIssuedThisTick = service.setback(lastLegal);
    }
}
