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

package com.deathmotion.totemguard.common.player.inventory.enums;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public enum EquipmentType {
    MAINHAND,
    OFFHAND,
    FEET,
    LEGS,
    CHEST,
    HEAD;

    public static EquipmentType getEquipmentSlotForItem(ItemStack itemStack) {
        ItemType item = itemStack.getType();
        if (item == ItemTypes.CARVED_PUMPKIN || (item.getName().getKey().contains("SKULL") ||
                (item.getName().getKey().contains("HEAD") && !item.getName().getKey().contains("PISTON")))) {
            return HEAD;
        }
        if (item == ItemTypes.ELYTRA) {
            return CHEST;
        }
        if (item == ItemTypes.LEATHER_BOOTS || item == ItemTypes.CHAINMAIL_BOOTS
                || item == ItemTypes.IRON_BOOTS || item == ItemTypes.DIAMOND_BOOTS
                || item == ItemTypes.GOLDEN_BOOTS || item == ItemTypes.NETHERITE_BOOTS
                || item == ItemTypes.COPPER_BOOTS) {
            return FEET;
        }
        if (item == ItemTypes.LEATHER_LEGGINGS || item == ItemTypes.CHAINMAIL_LEGGINGS
                || item == ItemTypes.IRON_LEGGINGS || item == ItemTypes.DIAMOND_LEGGINGS
                || item == ItemTypes.GOLDEN_LEGGINGS || item == ItemTypes.NETHERITE_LEGGINGS
                || item == ItemTypes.COPPER_LEGGINGS) {
            return LEGS;
        }
        if (item == ItemTypes.LEATHER_CHESTPLATE || item == ItemTypes.CHAINMAIL_CHESTPLATE
                || item == ItemTypes.IRON_CHESTPLATE || item == ItemTypes.DIAMOND_CHESTPLATE
                || item == ItemTypes.GOLDEN_CHESTPLATE || item == ItemTypes.NETHERITE_CHESTPLATE
                || item == ItemTypes.COPPER_CHESTPLATE) {
            return CHEST;
        }
        if (item == ItemTypes.LEATHER_HELMET || item == ItemTypes.CHAINMAIL_HELMET
                || item == ItemTypes.IRON_HELMET || item == ItemTypes.DIAMOND_HELMET
                || item == ItemTypes.GOLDEN_HELMET || item == ItemTypes.NETHERITE_HELMET
                || item == ItemTypes.COPPER_HELMET || item == ItemTypes.TURTLE_HELMET) {
            return HEAD;
        }
        return ItemTypes.SHIELD == item ? OFFHAND : MAINHAND;
    }
}
