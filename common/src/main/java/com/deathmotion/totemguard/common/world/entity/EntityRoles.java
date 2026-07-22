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

package com.deathmotion.totemguard.common.world.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public final class EntityRoles {

    private EntityRoles() {
    }

    public static boolean pushable(EntityType type) {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
    }

    public static boolean clientSidePusher(EntityType type) {
        if (type == null) return false;
        if (type == EntityTypes.ARMOR_STAND || type == EntityTypes.BAT
                || type == EntityTypes.PARROT || type == EntityTypes.ENDER_DRAGON) {
            return false;
        }
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT);
    }

    // Minecarts are pushable but NOT standable: vanilla gives them no hard collision box.
    public static boolean standable(EntityType type) {
        if (type == null) return false;
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)) return true;
        if (type == EntityTypes.SHULKER) return true;
        return type.getName() != null && "happy_ghast".equals(type.getName().getKey());
    }

    public static boolean boat(EntityType type) {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT);
    }

    public static boolean horseFamily(EntityType type) {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE) || camel(type);
    }

    public static boolean camel(EntityType type) {
        return type == EntityTypes.CAMEL || type == EntityTypes.CAMEL_HUSK;
    }

    public static boolean steerableMob(EntityType type) {
        return type == EntityTypes.PIG || type == EntityTypes.STRIDER;
    }

    public static boolean happyGhast(EntityType type) {
        return type == EntityTypes.HAPPY_GHAST;
    }

    public static boolean floatsWhileRidden(EntityType type) {
        return type == EntityTypes.HORSE || type == EntityTypes.ZOMBIE_HORSE
                || type == EntityTypes.MULE || type == EntityTypes.DONKEY
                || camel(type);
    }

    public static boolean clientAuthoritativeVehicle(EntityType type) {
        if (type == null) return false;
        return !EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
    }
}
