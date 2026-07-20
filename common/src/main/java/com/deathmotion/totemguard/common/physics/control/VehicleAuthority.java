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

package com.deathmotion.totemguard.common.physics.control;

import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.player.data.VehicleData;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public final class VehicleAuthority {

    private VehicleAuthority() {
    }

    public static boolean controlling(EntityType type, TrackedEntity ridden, VehicleData vehicle,
                                      EngineActor actor, VersionGates gates) {
        if (!vehicle.isDriverSeat()) return false;
        if (type == EntityTypes.LLAMA || type == EntityTypes.TRADER_LLAMA) return false;
        if (EntityRoles.steerableMob(type)) {
            boolean saddleGate = type == EntityTypes.PIG
                    ? gates.pigSaddleAuthority()
                    : gates.striderSaddleAuthority();
            boolean saddled = !ridden.saddleSeen() || ridden.saddled();
            return stickHeld(type, actor) && (!saddleGate || saddled);
        }
        if (EntityRoles.horseFamily(type)) {
            return !ridden.saddleSeen() || ridden.saddled();
        }
        if (EntityRoles.happyGhast(type)) {
            return ridden.harnessed() && !ridden.staysStill();
        }
        return true;
    }

    public static boolean simulating(EntityType type, TrackedEntity ridden, VehicleData vehicle,
                                     EngineActor actor, VersionGates gates) {
        if (controlling(type, ridden, vehicle, actor, gates)) return true;
        if (!vehicle.isDriverSeat()) return false;
        if (type == EntityTypes.LLAMA || type == EntityTypes.TRADER_LLAMA) return false;
        return EntityRoles.horseFamily(type) && !gates.horseSaddleAuthority();
    }

    public static boolean expectsMoves(EntityType type, TrackedEntity ridden, VehicleData vehicle,
                                       EngineActor actor, VersionGates gates) {
        if (!vehicle.isDriverSeat()) return false;
        if (type == EntityTypes.LLAMA || type == EntityTypes.TRADER_LLAMA) return false;
        if (EntityRoles.steerableMob(type)) {
            boolean saddleGate = type == EntityTypes.PIG
                    ? gates.pigSaddleAuthority()
                    : gates.striderSaddleAuthority();
            boolean saddled = !ridden.saddleSeen() || ridden.saddled();
            return stickHeld(type, actor) && (!saddleGate || saddled);
        }
        if (EntityRoles.horseFamily(type)) {
            boolean confirmed = ridden.saddleSeen() && ridden.saddled();
            return confirmed || !gates.horseSaddleAuthority();
        }
        if (EntityRoles.happyGhast(type)) {
            return ridden.harnessed() && !ridden.staysStill();
        }
        return true;
    }

    private static boolean stickHeld(EntityType type, EngineActor actor) {
        ItemType required = type == EntityTypes.PIG
                ? ItemTypes.CARROT_ON_A_STICK
                : ItemTypes.WARPED_FUNGUS_ON_A_STICK;
        return holds(actor.mainHandItem(), required) || holds(actor.offhandItem(), required);
    }

    private static boolean holds(ItemStack item, ItemType required) {
        return item != null && item.getType() == required;
    }
}
