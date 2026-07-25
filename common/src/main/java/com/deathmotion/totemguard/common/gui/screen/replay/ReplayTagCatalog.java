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

package com.deathmotion.totemguard.common.gui.screen.replay;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReplayTagCatalog {

    private static final List<Tag> KNOWN = List.of(
            new Tag("land", ItemTypes.STONE, "Walking, sprinting and jumping"),
            new Tag("water", ItemTypes.WATER_BUCKET, "Swimming and water drag"),
            new Tag("lava", ItemTypes.LAVA_BUCKET, "Lava movement"),
            new Tag("climb", ItemTypes.LADDER, "Ladders, vines and scaffolding climbs"),
            new Tag("glide", ItemTypes.ELYTRA, "Elytra flight"),
            new Tag("vehicle", ItemTypes.OAK_BOAT, "Boats, minecarts and mounts"),
            new Tag("entity", ItemTypes.NAME_TAG, "Entity push, collision and standing on entities"),
            new Tag("fluidbox", ItemTypes.BUCKET, "Fluid membership edge cases"),
            new Tag("piston", ItemTypes.PISTON, "Pistons moving blocks or the player"),
            new Tag("ice", ItemTypes.PACKED_ICE, "Ice friction"),
            new Tag("slime", ItemTypes.SLIME_BLOCK, "Slime bounces"),
            new Tag("honey", ItemTypes.HONEY_BLOCK, "Honey slowdown and wall sliding"),
            new Tag("scaffolding", ItemTypes.SCAFFOLDING, "Scaffolding towers"),
            new Tag("pearl", ItemTypes.ENDER_PEARL, "Ender pearl teleports"),
            new Tag("firework", ItemTypes.FIREWORK_ROCKET, "Firework boosts"),
            new Tag("riptide", ItemTypes.TRIDENT, "Riptide launches"),
            new Tag("knockback", ItemTypes.SNOWBALL, "Knockback and server velocity"),
            new Tag("portal", ItemTypes.OBSIDIAN, "Portals and world changes"),
            new Tag("timer", ItemTypes.CLOCK, "Tick rate and timer behaviour"),
            new Tag("inventory", ItemTypes.CHEST, "Inventory interaction"),
            new Tag("combat", ItemTypes.DIAMOND_SWORD, "Fighting and totem pops"));

    private static final Map<String, Tag> BY_NAME = index();

    private ReplayTagCatalog() {
    }

    private static Map<String, Tag> index() {
        Map<String, Tag> byName = new LinkedHashMap<>();
        for (Tag tag : KNOWN) byName.put(tag.name(), tag);
        return Map.copyOf(byName);
    }

    public static List<Tag> known() {
        return KNOWN;
    }

    public static Tag describe(String name) {
        Tag known = BY_NAME.get(name);
        return known == null ? new Tag(name, ItemTypes.NAME_TAG, "Custom tag") : known;
    }

    public record Tag(String name, ItemType item, String description) {
    }
}
