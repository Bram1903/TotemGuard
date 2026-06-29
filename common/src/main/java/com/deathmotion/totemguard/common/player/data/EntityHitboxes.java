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

package com.deathmotion.totemguard.common.player.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.HashMap;
import java.util.Map;

public final class EntityHitboxes {

    public static final double DEFAULT_WIDTH = 0.6;
    public static final double DEFAULT_HEIGHT = 1.8;

    private static final Map<String, double[]> SIZES = new HashMap<>();

    static {
        put("acacia_boat", 1.375, 0.5625);
        put("acacia_chest_boat", 1.375, 0.5625);
        put("allay", 0.35, 0.6);
        put("armadillo", 0.7, 0.65);
        put("armor_stand", 0.5, 1.975);
        put("axolotl", 0.75, 0.42);
        put("bamboo_chest_raft", 1.375, 0.5625);
        put("bamboo_raft", 1.375, 0.5625);
        put("bat", 0.5, 0.9);
        put("bee", 0.7, 0.6);
        put("birch_boat", 1.375, 0.5625);
        put("birch_chest_boat", 1.375, 0.5625);
        put("blaze", 0.6, 1.8);
        put("bogged", 0.6, 1.99);
        put("breeze", 0.6, 1.77);
        put("camel", 1.7, 2.375);
        put("camel_husk", 1.7, 2.375);
        put("cat", 0.6, 0.7);
        put("cave_spider", 0.7, 0.5);
        put("cherry_boat", 1.375, 0.5625);
        put("cherry_chest_boat", 1.375, 0.5625);
        put("chest_minecart", 0.98, 0.7);
        put("chicken", 0.4, 0.7);
        put("cod", 0.5, 0.3);
        put("command_block_minecart", 0.98, 0.7);
        put("copper_golem", 0.49, 0.98);
        put("cow", 0.9, 1.4);
        put("creaking", 0.9, 2.7);
        put("creeper", 0.6, 1.7);
        put("dark_oak_boat", 1.375, 0.5625);
        put("dark_oak_chest_boat", 1.375, 0.5625);
        put("dolphin", 0.9, 0.6);
        put("donkey", 1.3964844, 1.5);
        put("drowned", 0.6, 1.95);
        put("elder_guardian", 1.9975, 1.9975);
        put("ender_dragon", 16.0, 8.0);
        put("enderman", 0.6, 2.9);
        put("endermite", 0.4, 0.3);
        put("evoker", 0.6, 1.95);
        put("fox", 0.6, 0.7);
        put("frog", 0.5, 0.5);
        put("furnace_minecart", 0.98, 0.7);
        put("ghast", 4.0, 4.0);
        put("giant", 3.6, 12.0);
        put("glow_squid", 0.8, 0.8);
        put("goat", 0.9, 1.3);
        put("guardian", 0.85, 0.85);
        put("happy_ghast", 4.0, 4.0);
        put("hoglin", 1.3964844, 1.4);
        put("hopper_minecart", 0.98, 0.7);
        put("horse", 1.3964844, 1.6);
        put("husk", 0.6, 1.95);
        put("illusioner", 0.6, 1.95);
        put("iron_golem", 1.4, 2.7);
        put("jungle_boat", 1.375, 0.5625);
        put("jungle_chest_boat", 1.375, 0.5625);
        put("llama", 0.9, 1.87);
        put("magma_cube", 0.52, 0.52);
        put("mangrove_boat", 1.375, 0.5625);
        put("mangrove_chest_boat", 1.375, 0.5625);
        put("mannequin", 0.6, 1.8);
        put("minecart", 0.98, 0.7);
        put("mooshroom", 0.9, 1.4);
        put("mule", 1.3964844, 1.6);
        put("nautilus", 0.875, 0.95);
        put("oak_boat", 1.375, 0.5625);
        put("oak_chest_boat", 1.375, 0.5625);
        put("ocelot", 0.6, 0.7);
        put("pale_oak_boat", 1.375, 0.5625);
        put("pale_oak_chest_boat", 1.375, 0.5625);
        put("panda", 1.3, 1.25);
        put("parched", 0.6, 1.99);
        put("parrot", 0.5, 0.9);
        put("phantom", 0.9, 0.5);
        put("pig", 0.9, 0.9);
        put("piglin", 0.6, 1.95);
        put("piglin_brute", 0.6, 1.95);
        put("pillager", 0.6, 1.95);
        put("player", 0.6, 1.8);
        put("polar_bear", 1.4, 1.4);
        put("pufferfish", 0.7, 0.7);
        put("rabbit", 0.4, 0.5);
        put("ravager", 1.95, 2.2);
        put("salmon", 0.7, 0.4);
        put("sheep", 0.9, 1.3);
        put("shulker", 1.0, 1.0);
        put("silverfish", 0.4, 0.3);
        put("skeleton", 0.6, 1.99);
        put("skeleton_horse", 1.3964844, 1.6);
        put("slime", 0.52, 0.52);
        put("sniffer", 1.9, 1.75);
        put("snow_golem", 0.7, 1.9);
        put("spawner_minecart", 0.98, 0.7);
        put("spider", 1.4, 0.9);
        put("spruce_boat", 1.375, 0.5625);
        put("spruce_chest_boat", 1.375, 0.5625);
        put("squid", 0.8, 0.8);
        put("stray", 0.6, 1.99);
        put("strider", 0.9, 1.7);
        put("tadpole", 0.4, 0.3);
        put("tnt_minecart", 0.98, 0.7);
        put("trader_llama", 0.9, 1.87);
        put("tropical_fish", 0.5, 0.4);
        put("turtle", 1.2, 0.4);
        put("vex", 0.4, 0.8);
        put("villager", 0.6, 1.95);
        put("vindicator", 0.6, 1.95);
        put("wandering_trader", 0.6, 1.95);
        put("warden", 0.9, 2.9);
        put("witch", 0.6, 1.95);
        put("wither", 0.9, 3.5);
        put("wither_skeleton", 0.7, 2.4);
        put("wolf", 0.6, 0.85);
        put("zoglin", 1.3964844, 1.4);
        put("zombie", 0.6, 1.95);
        put("zombie_horse", 1.3964844, 1.6);
        put("zombie_nautilus", 0.875, 0.95);
        put("zombie_villager", 0.6, 1.95);
        put("zombified_piglin", 0.6, 1.95);
    }

    private EntityHitboxes() {
    }

    private static void put(String name, double width, double height) {
        SIZES.put(name, new double[]{width, height});
    }

    public static double width(EntityType type) {
        double[] size = lookup(type);
        return size == null ? DEFAULT_WIDTH : size[0];
    }

    public static double height(EntityType type) {
        double[] size = lookup(type);
        return size == null ? DEFAULT_HEIGHT : size[1];
    }

    private static double[] lookup(EntityType type) {
        if (type == null || type.getName() == null) return null;
        return SIZES.get(type.getName().getKey());
    }
}
