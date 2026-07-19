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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class VersionGates {

    private final boolean modernTrig;
    private final boolean waterEfficiencyAttribute;
    private final boolean sneakingSpeedAttribute;
    private final boolean swiftSneakInput;
    private final boolean speedFactorOnCenter;
    private final boolean modernFluidPush;
    private final boolean modernMovementThreshold;
    private final boolean modernBlockEffects;
    private final boolean restitutionBounce;
    private final boolean glideForceExitOnClimbable;
    private final boolean jointHorizontalZeroing;
    private final boolean floatWhileRidden;
    private final boolean modernStriderSuffocation;
    private final boolean supportingBlock;
    private final boolean useEffectsComponent;
    private final boolean claimedInput;
    private final boolean squareInputRescale;
    private final boolean doublePrecisionSprintBoost;
    private final boolean endTick;
    private final boolean horseSaddleAuthority;
    private final boolean pigSaddleAuthority;
    private final boolean striderSaddleAuthority;
    private final boolean boatSnapCollisionGate;

    private final boolean blockBreakComponentEra;
    private final boolean blockBreakAttributeEra;
    private final boolean creativeDestroyComponentEra;
    private final boolean maceCreativePenalty;
    private final boolean harvestOverride1214;
    private final boolean harvestOverride121;

    public VersionGates(ClientVersion client, boolean supportsEndTick) {
        this.modernTrig = client.isNewerThanOrEquals(ClientVersion.V_1_21_11);
        this.waterEfficiencyAttribute = client.isNewerThanOrEquals(ClientVersion.V_1_21);
        this.sneakingSpeedAttribute = client.isNewerThanOrEquals(ClientVersion.V_1_21);
        this.swiftSneakInput = client.isNewerThanOrEquals(ClientVersion.V_1_19);
        this.speedFactorOnCenter = client.isNewerThanOrEquals(ClientVersion.V_1_20_5);
        this.modernFluidPush = client.isNewerThanOrEquals(ClientVersion.V_26_1);
        this.modernMovementThreshold = client.isNewerThanOrEquals(ClientVersion.V_1_18_2);
        this.modernBlockEffects = client.isNewerThanOrEquals(ClientVersion.V_1_21_2);
        this.restitutionBounce = client.isNewerThanOrEquals(ClientVersion.V_26_2);
        this.glideForceExitOnClimbable = client.isNewerThanOrEquals(ClientVersion.V_1_21_5)
                && client.isOlderThan(ClientVersion.V_1_21_11);
        this.jointHorizontalZeroing = client.isNewerThanOrEquals(ClientVersion.V_1_21_5);
        this.floatWhileRidden = client.isNewerThanOrEquals(ClientVersion.V_1_21_11);
        this.modernStriderSuffocation = client.isNewerThanOrEquals(ClientVersion.V_1_19_4);
        this.supportingBlock = client.isNewerThanOrEquals(ClientVersion.V_1_20);
        this.useEffectsComponent = client.isNewerThanOrEquals(ClientVersion.V_1_21_11);
        this.claimedInput = client.isNewerThanOrEquals(ClientVersion.V_1_21_2);
        this.squareInputRescale = client.isNewerThanOrEquals(ClientVersion.V_1_21_5);
        this.doublePrecisionSprintBoost = client.isNewerThanOrEquals(ClientVersion.V_1_20_5);
        this.endTick = supportsEndTick;
        this.horseSaddleAuthority = client.isNewerThanOrEquals(ClientVersion.V_1_19);
        this.pigSaddleAuthority = client.isNewerThanOrEquals(ClientVersion.V_1_19);
        this.striderSaddleAuthority = client.isNewerThanOrEquals(ClientVersion.V_1_20_2);
        this.boatSnapCollisionGate = client.isNewerThanOrEquals(ClientVersion.V_1_21);

        ServerVersion server = PacketEvents.getAPI().getServerManager().getVersion();
        boolean componentEra = server.isNewerThanOrEquals(ServerVersion.V_1_20_5)
                && client.isNewerThanOrEquals(ClientVersion.V_1_20_5);
        this.blockBreakComponentEra = componentEra;
        this.blockBreakAttributeEra = server.isNewerThanOrEquals(ServerVersion.V_1_21)
                && client.isNewerThanOrEquals(ClientVersion.V_1_21);
        this.creativeDestroyComponentEra = componentEra && client.isNewerThanOrEquals(ClientVersion.V_1_21_5);
        this.maceCreativePenalty = client.isNewerThanOrEquals(ClientVersion.V_1_20_5);
        this.harvestOverride1214 = client.isNewerThanOrEquals(ClientVersion.V_1_21_4);
        this.harvestOverride121 = client.isNewerThanOrEquals(ClientVersion.V_1_21);
    }
}
