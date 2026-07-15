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

package com.deathmotion.totemguard.common.util;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class MetadataIndex {

    private final int health;
    private final int absorption;
    private final int slimeSize;
    private final int pose;
    private final int sleepingPos;
    private final int livingEntityFlags;
    private final int fireworkAttached;
    private final int fireworkItem;
    private final int ticksFrozen;
    private final int hookedEntity;
    private final int pigBoostTime;
    private final int striderBoostTime;
    private final int striderSuffocating;

    public MetadataIndex(ClientVersion version) {
        this.health = resolveHealth(version);
        this.absorption = resolveAbsorption(version);
        this.slimeSize = resolveSlimeSize(version);
        this.pose = resolvePose(version);
        this.sleepingPos = resolveSleepingPos(version);
        this.livingEntityFlags = resolveLivingEntityFlags(version);
        this.fireworkAttached = resolveFireworkAttached(version);
        this.fireworkItem = resolveFireworkItem(version);
        this.ticksFrozen = resolveTicksFrozen(version);
        this.hookedEntity = resolveHookedEntity(version);
        this.pigBoostTime = resolvePigBoostTime(version);
        this.striderBoostTime = resolveStriderBoostTime(version);
        this.striderSuffocating = resolveStriderSuffocating(version);
    }

    private static int resolveHealth(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 9;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 8;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_10)) return 7;
        return 6;
    }

    private static int resolveAbsorption(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_21_9)) return 17;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 15;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_15)) return 14;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 13;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_10)) return 11;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9)) return 10;
        return 17;
    }

    private static int resolveSlimeSize(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 16;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_15)) return 15;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 14;
        return -1;
    }

    private static int resolvePose(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 6;
        return -1;
    }

    private static int resolveSleepingPos(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 14;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_15)) return 13;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 12;
        return -1;
    }

    private static int resolveLivingEntityFlags(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 8;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 7;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_10)) return 6;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9)) return 5;
        return -1;
    }

    private static int resolveFireworkAttached(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 9;
        return -1;
    }

    private static int resolveTicksFrozen(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 7;
        return -1;
    }

    private static int resolveHookedEntity(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 8;
        return -1;
    }

    private static int resolveFireworkItem(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 8;
        return -1;
    }

    private static int resolvePigBoostTime(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_26_1)) return 18;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_21_5)) return 17;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 18;
        return -1;
    }

    private static int resolveStriderBoostTime(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_26_1)) return 18;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 17;
        return -1;
    }

    private static int resolveStriderSuffocating(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_26_1)) return 19;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 18;
        return -1;
    }
}
