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

public final class MetadataIndex {

    private final int health;
    private final int absorption;
    private final int slimeSize;
    private final int livingEntityFlags;

    public MetadataIndex(ClientVersion version) {
        this.health = resolveHealth(version);
        this.absorption = resolveAbsorption(version);
        this.slimeSize = resolveSlimeSize(version);
        this.livingEntityFlags = resolveLivingEntityFlags(version);
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

    private static int resolveLivingEntityFlags(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) return 8;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_14)) return 7;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_10)) return 6;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9)) return 5;
        return -1;
    }

    public int health() {
        return health;
    }

    public int absorption() {
        return absorption;
    }

    public int slimeSize() {
        return slimeSize;
    }

    public int livingEntityFlags() {
        return livingEntityFlags;
    }
}
