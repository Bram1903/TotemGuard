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

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class VersionGates {

    private final boolean modernTrig;
    private final boolean waterEfficiencyAttribute;
    private final boolean speedFactorOnCenter;
    private final boolean modernFluidPush;
    private final boolean floatWhileRidden;
    private final boolean endTick;

    public VersionGates(ClientVersion client, boolean supportsEndTick) {
        this.modernTrig = client.isNewerThanOrEquals(ClientVersion.V_1_21_11);
        this.waterEfficiencyAttribute = client.isNewerThanOrEquals(ClientVersion.V_1_21);
        this.speedFactorOnCenter = client.isNewerThanOrEquals(ClientVersion.V_1_20_5);
        this.modernFluidPush = client.isNewerThanOrEquals(ClientVersion.V_26_1);
        this.floatWhileRidden = client.isNewerThanOrEquals(ClientVersion.V_1_21_11);
        this.endTick = supportsEndTick;
    }
}
