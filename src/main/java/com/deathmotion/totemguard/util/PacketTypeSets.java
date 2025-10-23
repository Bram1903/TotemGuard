/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.util;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public final class PacketTypeSets {

    public static final Set<PacketTypeCommon> PLAYER_TRIGGERED = Set.of(
            PacketType.Play.Client.INTERACT_ENTITY,
            PacketType.Play.Client.ENTITY_ACTION,
            PacketType.Play.Client.PLAYER_DIGGING,
            PacketType.Play.Client.USE_ITEM,
            PacketType.Play.Client.STEER_VEHICLE,
            PacketType.Play.Client.CLICK_WINDOW,
            PacketType.Play.Client.CLOSE_WINDOW
    );
}
