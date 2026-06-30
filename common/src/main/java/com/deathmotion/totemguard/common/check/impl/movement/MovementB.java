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

package com.deathmotion.totemguard.common.check.impl.movement;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.deathmotion.totemguard.common.player.movement.MovementEstimator;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@RequiresTickEnd
@CheckData(description = "Horizontal movement without input", type = CheckType.MOVEMENT, experimental = true)
public class MovementB extends CheckImpl implements PacketCheck {

    private static final int STREAK_TO_FLAG = 2;

    private final InputData inputData;
    private final MovementEstimator movementEstimator;

    private int streak;

    public MovementB(TGPlayer player) {
        super(player);
        this.inputData = player.getData().getInputData();
        this.movementEstimator = player.getData().getMovementEstimator();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (inputData.hasMovement(true)) {
            streak = 0;
            return;
        }

        if (!movementEstimator.isMovedThisTick()) {
            if (streak > 0) streak--;
            return;
        }

        if (++streak >= STREAK_TO_FLAG) {
            streak = 0;
            fail("ticks={0}", STREAK_TO_FLAG);
        }
    }
}
