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

import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.player.data.Data;

public final class SwimTracker {

    private boolean lastSprinting;
    private boolean lastEyeInWater;
    private long lastEchoSeq;

    public void update(Data data, MediumSample sample) {
        boolean sprinting = data.isSprinting();
        boolean eyeInWater = sample.eyeInWater();
        long echoSeq = data.getSharedFlagsEchoSeq();
        boolean echoed = echoSeq != lastEchoSeq;
        lastEchoSeq = echoSeq;

        boolean wasSwimming = echoed ? data.isEchoedSwimming() : data.isSwimming();
        boolean readSprinting = echoed ? data.isEchoedSprinting() : lastSprinting;

        boolean swimming;
        if (data.isFlying() || data.isInVehicle()) {
            swimming = false;
        } else if (wasSwimming) {
            swimming = readSprinting && sample.water();
        } else {
            swimming = readSprinting && lastEyeInWater && sample.waterAtFeet();
        }
        data.setSwimming(swimming);
        lastSprinting = sprinting;
        lastEyeInWater = eyeInWater;
    }

    public void reset() {
        lastSprinting = false;
        lastEyeInWater = false;
    }
}
