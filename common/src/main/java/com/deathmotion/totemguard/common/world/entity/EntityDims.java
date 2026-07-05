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

import java.util.HashMap;
import java.util.Map;

public final class EntityDims {

    public static final double DEFAULT_WIDTH = 0.6;
    public static final double DEFAULT_HEIGHT = 1.8;

    private static final double BOAT_WIDTH = 1.375;
    private static final double BOAT_HEIGHT = 0.5625;
    private static final double MINECART_WIDTH = 0.98;
    private static final double MINECART_HEIGHT = 0.7;
    private static final double HORSE_WIDTH = 1.3964844;

    private static final Map<EntityType, Dims> DIMS = new HashMap<>(256);

    private record Dims(double width, double height) {
    }

    static {
        put(EntityTypes.ALLAY, 0.35, 0.6);
        put(EntityTypes.AREA_EFFECT_CLOUD, 6.0, 0.5);
        put(EntityTypes.ARMADILLO, 0.7, 0.65);
        put(EntityTypes.ARMOR_STAND, 0.5, 1.975);
        put(EntityTypes.ARROW, 0.5, 0.5);
        put(EntityTypes.AXOLOTL, 0.75, 0.42);
        put(EntityTypes.BAT, 0.5, 0.9);
        put(EntityTypes.BEE, 0.55, 0.5);
        put(EntityTypes.BLAZE, 0.6, 1.8);
        put(EntityTypes.BLOCK_DISPLAY, 0.0, 0.0);
        put(EntityTypes.BOGGED, 0.6, 1.99);
        put(EntityTypes.BREEZE, 0.6, 1.77);
        put(EntityTypes.BREEZE_WIND_CHARGE, 0.3125, 0.3125);
        put(EntityTypes.CAMEL, 1.7, 2.375);
        put(EntityTypes.CAMEL_HUSK, 1.7, 2.375);
        put(EntityTypes.CAT, 0.6, 0.7);
        put(EntityTypes.CAVE_SPIDER, 0.7, 0.5);
        put(EntityTypes.CHICKEN, 0.4, 0.7);
        put(EntityTypes.COD, 0.5, 0.3);
        put(EntityTypes.COPPER_GOLEM, 0.49, 0.98);
        put(EntityTypes.COW, 0.9, 1.4);
        put(EntityTypes.CREAKING, 0.9, 2.7);
        put(EntityTypes.CREEPER, 0.6, 1.7);
        put(EntityTypes.DOLPHIN, 0.9, 0.6);
        put(EntityTypes.DONKEY, HORSE_WIDTH, 1.5);
        put(EntityTypes.DRAGON_FIREBALL, 1.0, 1.0);
        put(EntityTypes.DROWNED, 0.6, 1.95);
        put(EntityTypes.EGG, 0.25, 0.25);
        put(EntityTypes.ELDER_GUARDIAN, 1.9975, 1.9975);
        put(EntityTypes.END_CRYSTAL, 2.0, 2.0);
        put(EntityTypes.ENDER_DRAGON, 16.0, 8.0);
        put(EntityTypes.ENDER_PEARL, 0.25, 0.25);
        put(EntityTypes.ENDERMAN, 0.6, 2.9);
        put(EntityTypes.ENDERMITE, 0.4, 0.3);
        put(EntityTypes.EVOKER, 0.6, 1.95);
        put(EntityTypes.EVOKER_FANGS, 0.5, 0.8);
        put(EntityTypes.EXPERIENCE_BOTTLE, 0.25, 0.25);
        put(EntityTypes.EXPERIENCE_ORB, 0.5, 0.5);
        put(EntityTypes.EYE_OF_ENDER, 0.25, 0.25);
        put(EntityTypes.FALLING_BLOCK, 0.98, 0.98);
        put(EntityTypes.FIREBALL, 1.0, 1.0);
        put(EntityTypes.FIREWORK_ROCKET, 0.25, 0.25);
        put(EntityTypes.FISHING_BOBBER, 0.25, 0.25);
        put(EntityTypes.FOX, 0.6, 0.7);
        put(EntityTypes.FROG, 0.5, 0.5);
        put(EntityTypes.GHAST, 4.0, 4.0);
        put(EntityTypes.GIANT, 3.6, 12.0);
        put(EntityTypes.GLOW_ITEM_FRAME, 0.5, 0.5);
        put(EntityTypes.GLOW_SQUID, 0.8, 0.8);
        put(EntityTypes.GOAT, 0.9, 1.3);
        put(EntityTypes.GUARDIAN, 0.85, 0.85);
        put(EntityTypes.HAPPY_GHAST, 4.0, 4.0);
        put(EntityTypes.HOGLIN, HORSE_WIDTH, 1.4);
        put(EntityTypes.HORSE, HORSE_WIDTH, 1.6);
        put(EntityTypes.HUSK, 0.6, 1.95);
        put(EntityTypes.ILLUSIONER, 0.6, 1.95);
        put(EntityTypes.INTERACTION, 0.0, 0.0);
        put(EntityTypes.IRON_GOLEM, 1.4, 2.7);
        put(EntityTypes.ITEM, 0.25, 0.25);
        put(EntityTypes.ITEM_DISPLAY, 0.0, 0.0);
        put(EntityTypes.ITEM_FRAME, 0.5, 0.5);
        put(EntityTypes.LEASH_KNOT, 0.375, 0.5);
        put(EntityTypes.LIGHTNING_BOLT, 0.0, 0.0);
        put(EntityTypes.LLAMA, 0.9, 1.87);
        put(EntityTypes.LLAMA_SPIT, 0.25, 0.25);
        put(EntityTypes.MAGMA_CUBE, 0.52, 0.52);
        put(EntityTypes.MANNEQUIN, 0.6, 1.8);
        put(EntityTypes.MARKER, 0.0, 0.0);
        put(EntityTypes.MOOSHROOM, 0.9, 1.4);
        put(EntityTypes.MULE, HORSE_WIDTH, 1.6);
        put(EntityTypes.NAUTILUS, 0.875, 0.95);
        put(EntityTypes.OCELOT, 0.6, 0.7);
        put(EntityTypes.OMINOUS_ITEM_SPAWNER, 0.25, 0.25);
        put(EntityTypes.PAINTING, 0.5, 0.5);
        put(EntityTypes.PANDA, 1.3, 1.25);
        put(EntityTypes.PARCHED, 0.6, 1.99);
        put(EntityTypes.PARROT, 0.5, 0.9);
        put(EntityTypes.PHANTOM, 0.9, 0.5);
        put(EntityTypes.PIG, 0.9, 0.9);
        put(EntityTypes.PIGLIN, 0.6, 1.95);
        put(EntityTypes.PIGLIN_BRUTE, 0.6, 1.95);
        put(EntityTypes.PILLAGER, 0.6, 1.95);
        put(EntityTypes.PLAYER, 0.6, 1.8);
        put(EntityTypes.POLAR_BEAR, 1.4, 1.4);
        put(EntityTypes.PUFFERFISH, 0.7, 0.7);
        put(EntityTypes.RABBIT, 0.49, 0.6);
        put(EntityTypes.RAVAGER, 1.95, 2.2);
        put(EntityTypes.SALMON, 0.7, 0.4);
        put(EntityTypes.SHEEP, 0.9, 1.3);
        put(EntityTypes.SHULKER, 1.0, 1.0);
        put(EntityTypes.SHULKER_BULLET, 0.3125, 0.3125);
        put(EntityTypes.SILVERFISH, 0.4, 0.3);
        put(EntityTypes.SKELETON, 0.6, 1.99);
        put(EntityTypes.SKELETON_HORSE, HORSE_WIDTH, 1.6);
        put(EntityTypes.SLIME, 0.52, 0.52);
        put(EntityTypes.SMALL_FIREBALL, 0.3125, 0.3125);
        put(EntityTypes.SNIFFER, 1.9, 1.75);
        put(EntityTypes.SNOW_GOLEM, 0.7, 1.9);
        put(EntityTypes.SNOWBALL, 0.25, 0.25);
        put(EntityTypes.SPECTRAL_ARROW, 0.5, 0.5);
        put(EntityTypes.SPIDER, 1.4, 0.9);
        put(EntityTypes.SQUID, 0.8, 0.8);
        put(EntityTypes.STRAY, 0.6, 1.99);
        put(EntityTypes.STRIDER, 0.9, 1.7);
        put(EntityTypes.SULFUR_CUBE, 0.49, 0.49);
        put(EntityTypes.TADPOLE, 0.4, 0.3);
        put(EntityTypes.TEXT_DISPLAY, 0.0, 0.0);
        put(EntityTypes.TRADER_LLAMA, 0.9, 1.87);
        put(EntityTypes.TRIDENT, 0.5, 0.5);
        put(EntityTypes.TROPICAL_FISH, 0.5, 0.4);
        put(EntityTypes.TURTLE, 1.2, 0.4);
        put(EntityTypes.VEX, 0.4, 0.8);
        put(EntityTypes.VILLAGER, 0.6, 1.95);
        put(EntityTypes.VINDICATOR, 0.6, 1.95);
        put(EntityTypes.WANDERING_TRADER, 0.6, 1.95);
        put(EntityTypes.WARDEN, 0.9, 2.9);
        put(EntityTypes.WIND_CHARGE, 0.3125, 0.3125);
        put(EntityTypes.WITCH, 0.6, 1.95);
        put(EntityTypes.WITHER, 0.9, 3.5);
        put(EntityTypes.WITHER_SKELETON, 0.7, 2.4);
        put(EntityTypes.WITHER_SKULL, 0.3125, 0.3125);
        put(EntityTypes.WOLF, 0.6, 0.85);
        put(EntityTypes.ZOGLIN, HORSE_WIDTH, 1.4);
        put(EntityTypes.ZOMBIE, 0.6, 1.95);
        put(EntityTypes.ZOMBIE_HORSE, HORSE_WIDTH, 1.6);
        put(EntityTypes.ZOMBIE_NAUTILUS, 0.875, 0.95);
        put(EntityTypes.ZOMBIE_VILLAGER, 0.6, 1.95);
        put(EntityTypes.ZOMBIFIED_PIGLIN, 0.6, 1.95);

        put(EntityTypes.PRIMED_TNT, 0.98, 0.98);
        put(EntityTypes.SPLASH_POTION, 0.25, 0.25);
        put(EntityTypes.LINGERING_POTION, 0.25, 0.25);
        put(EntityTypes.POTION, 0.25, 0.25);
        put(EntityTypes.BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);

        put(EntityTypes.ACACIA_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.ACACIA_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.BAMBOO_RAFT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.BAMBOO_CHEST_RAFT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.BIRCH_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.BIRCH_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.CHERRY_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.CHERRY_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.DARK_OAK_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.DARK_OAK_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.JUNGLE_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.JUNGLE_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.MANGROVE_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.MANGROVE_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.OAK_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.OAK_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.PALE_OAK_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.PALE_OAK_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.SPRUCE_BOAT, BOAT_WIDTH, BOAT_HEIGHT);
        put(EntityTypes.SPRUCE_CHEST_BOAT, BOAT_WIDTH, BOAT_HEIGHT);

        put(EntityTypes.MINECART, MINECART_WIDTH, MINECART_HEIGHT);
        put(EntityTypes.CHEST_MINECART, MINECART_WIDTH, MINECART_HEIGHT);
        put(EntityTypes.COMMAND_BLOCK_MINECART, MINECART_WIDTH, MINECART_HEIGHT);
        put(EntityTypes.FURNACE_MINECART, MINECART_WIDTH, MINECART_HEIGHT);
        put(EntityTypes.HOPPER_MINECART, MINECART_WIDTH, MINECART_HEIGHT);
        put(EntityTypes.SPAWNER_MINECART, MINECART_WIDTH, MINECART_HEIGHT);
        put(EntityTypes.TNT_MINECART, MINECART_WIDTH, MINECART_HEIGHT);
    }

    private EntityDims() {
    }

    private static void put(EntityType type, double width, double height) {
        DIMS.put(type, new Dims(width, height));
    }

    public static double width(EntityType type) {
        Dims dims = type == null ? null : DIMS.get(type);
        return dims == null ? DEFAULT_WIDTH : dims.width();
    }

    public static double height(EntityType type) {
        Dims dims = type == null ? null : DIMS.get(type);
        return dims == null ? DEFAULT_HEIGHT : dims.height();
    }
}
