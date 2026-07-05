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

package com.deathmotion.totemguard.common.world.shape;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class ShapeRegistry {

    private static final Map<StateType, ShapeResolver> RESOLVERS = new HashMap<>(1024);
    private static final Set<StateType> APPROXIMATE = new HashSet<>();
    private static final Set<StateType> SUFFOCATING_ALWAYS = new HashSet<>();
    private static final Set<StateType> SUFFOCATING_NEVER = new HashSet<>();

    private static final double FULL_CUBE_EPS = 1.0e-7;

    static {
        register();
    }

    private ShapeRegistry() {
    }

    public static void collect(WrappedBlockState state, int x, int y, int z, ShapeQuery query, ShapeSink sink) {
        StateType type = state.getType();
        if (type == StateTypes.AIR || type == StateTypes.WATER || type == StateTypes.LAVA) return;
        ShapeResolver resolver = RESOLVERS.get(type);
        if (resolver != null) {
            resolver.collect(state, x, y, z, query, sink);
            return;
        }
        if (type.isBlocking() && type.isSolid()) {
            ShapeCatalog.emit(ShapeCatalog.FULL, x, y, z, sink);
        }
    }

    public static boolean fullCubeDefault(WrappedBlockState state) {
        double[] bounds = new double[6];
        int[] count = {0};
        collect(state, 0, 0, 0, ShapeQuery.DEFAULT, (minX, minY, minZ, maxX, maxY, maxZ) -> {
            if (count[0]++ == 0) {
                bounds[0] = minX;
                bounds[1] = minY;
                bounds[2] = minZ;
                bounds[3] = maxX;
                bounds[4] = maxY;
                bounds[5] = maxZ;
            }
        });
        return count[0] == 1
                && bounds[0] <= FULL_CUBE_EPS && bounds[1] <= FULL_CUBE_EPS && bounds[2] <= FULL_CUBE_EPS
                && bounds[3] >= 1.0 - FULL_CUBE_EPS && bounds[4] >= 1.0 - FULL_CUBE_EPS && bounds[5] >= 1.0 - FULL_CUBE_EPS;
    }

    public static boolean hasShape(WrappedBlockState state) {
        boolean[] any = {false};
        collect(state, 0, 0, 0, ShapeQuery.DEFAULT, (a, b, c, d, e, f) -> any[0] = true);
        return any[0];
    }

    public static boolean wallTrusted(StateType type) {
        return !APPROXIMATE.contains(type);
    }

    public static boolean supportApproximate(StateType type) {
        return APPROXIMATE.contains(type);
    }

    public static boolean suffocatingOverride(StateType type) {
        return SUFFOCATING_ALWAYS.contains(type);
    }

    public static boolean suffocatingNever(StateType type) {
        return SUFFOCATING_NEVER.contains(type);
    }

    private static void register() {
        tag(BlockTags.SLABS, byState(ShapeVariants::slab));
        tag(BlockTags.STAIRS, (state, x, y, z, q, sink) -> ShapeVariants.stairs(state, x, y, z, sink));
        tag(BlockTags.FENCES, (state, x, y, z, q, sink) -> ShapeVariants.fence(state, x, y, z, sink));
        tag(BlockTags.FENCE_GATES, (state, x, y, z, q, sink) -> ShapeVariants.fenceGate(state, x, y, z, sink));
        tag(BlockTags.WALLS, (state, x, y, z, q, sink) -> ShapeVariants.wall(state, x, y, z, sink));
        tag(BlockTags.GLASS_PANES, (state, x, y, z, q, sink) -> ShapeVariants.pane(state, x, y, z, sink));
        tag(BlockTags.BARS, (state, x, y, z, q, sink) -> ShapeVariants.pane(state, x, y, z, sink));
        tag(BlockTags.DOORS, byState(ShapeVariants::door));
        tag(BlockTags.TRAPDOORS, byState(ShapeVariants::trapdoor));
        tag(BlockTags.BEDS, byState(ShapeVariants::bed));
        tag(BlockTags.CANDLES, byState(ShapeVariants::candles));
        tag(BlockTags.CANDLE_CAKES, fixed(ShapeCatalog.CANDLE_CAKE));
        tag(BlockTags.CAMPFIRES, fixed(ShapeCatalog.CAMPFIRE));
        tag(BlockTags.ANVIL, byState(ShapeVariants::anvil));
        tag(BlockTags.WOOL_CARPETS, fixed(ShapeCatalog.CARPET));
        tag(BlockTags.CAULDRONS, fixed(ShapeCatalog.CAULDRON));
        tag(BlockTags.FLOWER_POTS, fixed(ShapeCatalog.FLOWER_POT));
        tag(BlockTags.LANTERNS, byState(ShapeVariants::lantern));
        tag(BlockTags.CHAINS, byState(ShapeVariants::chain));
        tag(BlockTags.LIGHTNING_RODS, byState(ShapeVariants::rod));
        tag(BlockTags.WOODEN_SHELVES, byState(ShapeVariants::shelf));
        tag(BlockTags.WALL_HANGING_SIGNS, byState(ShapeVariants::wallHangingSign));
        tag(BlockTags.COPPER_GOLEM_STATUES, fixed(ShapeCatalog.GOLEM_STATUE));
        tag(BlockTags.SPELEOTHEMS, (state, x, y, z, q, sink) -> ShapeVariants.speleothem(state, x, y, z, sink));

        tag(BlockTags.SHULKER_BOXES, fixed(ShapeCatalog.FULL));
        BlockTags.SHULKER_BOXES.getStates().forEach(APPROXIMATE::add);

        fixed(ShapeCatalog.SOUL_SAND, StateTypes.SOUL_SAND);
        fixed(ShapeCatalog.MUD, StateTypes.MUD);
        fixed(ShapeCatalog.PATH, StateTypes.FARMLAND, StateTypes.DIRT_PATH, StateTypes.GRASS_PATH);
        fixed(ShapeCatalog.ENCHANTING_TABLE, StateTypes.ENCHANTING_TABLE);
        fixed(ShapeCatalog.DAYLIGHT_DETECTOR, StateTypes.DAYLIGHT_DETECTOR);
        fixed(ShapeCatalog.DIODE, StateTypes.REPEATER, StateTypes.COMPARATOR);
        fixed(ShapeCatalog.STONECUTTER, StateTypes.STONECUTTER);
        fixed(ShapeCatalog.SCULK_SENSOR, StateTypes.SCULK_SENSOR, StateTypes.CALIBRATED_SCULK_SENSOR, StateTypes.SCULK_SHRIEKER);
        fixed(ShapeCatalog.HONEY_CACTUS, StateTypes.HONEY_BLOCK, StateTypes.CACTUS);
        fixed(ShapeCatalog.LILY_PAD, StateTypes.LILY_PAD);
        fixed(ShapeCatalog.CARPET, StateTypes.MOSS_CARPET);
        fixed(ShapeCatalog.FULL_HEIGHT_POT, StateTypes.DECORATED_POT, StateTypes.DRAGON_EGG);
        fixed(ShapeCatalog.CONDUIT, StateTypes.CONDUIT);
        fixed(ShapeCatalog.HEAVY_CORE, StateTypes.HEAVY_CORE);
        fixed(ShapeCatalog.DRIED_GHAST, StateTypes.DRIED_GHAST);
        fixed(ShapeCatalog.SNIFFER_EGG, StateTypes.SNIFFER_EGG);
        fixed(ShapeCatalog.AZALEA, StateTypes.AZALEA, StateTypes.FLOWERING_AZALEA);
        fixed(ShapeCatalog.COMPOSTER, StateTypes.COMPOSTER);
        fixed(ShapeCatalog.LECTERN, StateTypes.LECTERN);
        fixed(ShapeCatalog.BREWING_STAND, StateTypes.BREWING_STAND);
        fixed(ShapeCatalog.SKULL,
                StateTypes.SKELETON_SKULL, StateTypes.WITHER_SKELETON_SKULL, StateTypes.ZOMBIE_HEAD,
                StateTypes.PLAYER_HEAD, StateTypes.CREEPER_HEAD, StateTypes.DRAGON_HEAD);
        fixed(ShapeCatalog.PIGLIN_SKULL, StateTypes.PIGLIN_HEAD);
        fixed(ShapeCatalog.EMPTY, StateTypes.LIGHT);

        byState(ShapeVariants::snowLayers, StateTypes.SNOW);
        RESOLVERS.put(StateTypes.CAKE, (state, x, y, z, q, sink) -> ShapeVariants.cake(state, x, y, z, sink));
        byState(ShapeVariants::ladder, StateTypes.LADDER);
        byState(ShapeVariants::chest, StateTypes.CHEST, StateTypes.TRAPPED_CHEST);
        tag(BlockTags.COPPER_CHESTS, byState(ShapeVariants::chest));
        fixed(ShapeCatalog.CHEST_SINGLE, StateTypes.ENDER_CHEST);
        byState(ShapeVariants::endPortalFrame, StateTypes.END_PORTAL_FRAME);
        byState(ShapeVariants::turtleEgg, StateTypes.TURTLE_EGG);
        byState(ShapeVariants::seaPickle, StateTypes.SEA_PICKLE);
        byState(ShapeVariants::hopper, StateTypes.HOPPER);
        byState(ShapeVariants::wallSkull,
                StateTypes.SKELETON_WALL_SKULL, StateTypes.WITHER_SKELETON_WALL_SKULL,
                StateTypes.ZOMBIE_WALL_HEAD, StateTypes.PLAYER_WALL_HEAD,
                StateTypes.CREEPER_WALL_HEAD, StateTypes.DRAGON_WALL_HEAD);
        byState(ShapeVariants::piglinWallSkull, StateTypes.PIGLIN_WALL_HEAD);
        byState(ShapeVariants::bell, StateTypes.BELL);
        byState(ShapeVariants::grindstone, StateTypes.GRINDSTONE);
        byState(ShapeVariants::pistonBase, StateTypes.PISTON, StateTypes.STICKY_PISTON);
        byState(ShapeVariants::pistonHead, StateTypes.PISTON_HEAD);
        RESOLVERS.put(StateTypes.CHORUS_PLANT, (state, x, y, z, q, sink) -> ShapeVariants.chorusPlant(state, x, y, z, sink));
        byState(ShapeVariants::bigDripleaf, StateTypes.BIG_DRIPLEAF);
        byState(s -> ShapeVariants.amethystShape(s, ShapeVariants.amethystCluster()), StateTypes.AMETHYST_CLUSTER);
        byState(s -> ShapeVariants.amethystShape(s, ShapeVariants.largeAmethystBud()), StateTypes.LARGE_AMETHYST_BUD);
        byState(s -> ShapeVariants.amethystShape(s, ShapeVariants.mediumAmethystBud()), StateTypes.MEDIUM_AMETHYST_BUD);
        byState(s -> ShapeVariants.amethystShape(s, ShapeVariants.smallAmethystBud()), StateTypes.SMALL_AMETHYST_BUD);
        byState(ShapeVariants::cocoa, StateTypes.COCOA);
        byState(ShapeVariants::pitcherCrop, StateTypes.PITCHER_CROP);
        byState(s -> s.isBottom() ? ShapeCatalog.CARPET : ShapeCatalog.EMPTY, StateTypes.PALE_MOSS_CARPET);
        byState(ShapeVariants::rod, StateTypes.END_ROD);
        byState(ShapeVariants::chain, StateTypes.CHAIN);

        RESOLVERS.put(StateTypes.BAMBOO, (state, x, y, z, q, sink) -> ShapeVariants.bamboo(x, y, z, sink));
        RESOLVERS.put(StateTypes.SCAFFOLDING, (state, x, y, z, q, sink) -> ShapeVariants.scaffolding(state, x, y, z, q, sink));
        APPROXIMATE.add(StateTypes.SCAFFOLDING);

        RESOLVERS.put(StateTypes.POWDER_SNOW, (state, x, y, z, q, sink) -> ShapeVariants.powderSnow(x, y, z, q, sink));
        APPROXIMATE.add(StateTypes.POWDER_SNOW);

        fixed(ShapeCatalog.FULL, StateTypes.MOVING_PISTON);
        APPROXIMATE.add(StateTypes.MOVING_PISTON);

        SUFFOCATING_ALWAYS.add(StateTypes.SOUL_SAND);
        SUFFOCATING_ALWAYS.add(StateTypes.MUD);
        SUFFOCATING_ALWAYS.add(StateTypes.FARMLAND);
        SUFFOCATING_ALWAYS.add(StateTypes.DIRT_PATH);
        SUFFOCATING_ALWAYS.add(StateTypes.GRASS_PATH);

        BlockTags.LEAVES.getStates().forEach(SUFFOCATING_NEVER::add);
        SUFFOCATING_NEVER.add(StateTypes.GLASS);
        SUFFOCATING_NEVER.add(StateTypes.TINTED_GLASS);
        BlockTags.GLASS_BLOCKS.getStates().forEach(SUFFOCATING_NEVER::add);
        SUFFOCATING_NEVER.add(StateTypes.MANGROVE_ROOTS);
        SUFFOCATING_NEVER.add(StateTypes.MOVING_PISTON);
        SUFFOCATING_NEVER.add(StateTypes.COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.EXPOSED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WEATHERED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.OXIDIZED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_EXPOSED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_WEATHERED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_OXIDIZED_COPPER_GRATE);
    }

    private static ShapeResolver fixed(double[] boxes) {
        return (state, x, y, z, q, sink) -> ShapeCatalog.emit(boxes, x, y, z, sink);
    }

    private static ShapeResolver byState(Function<WrappedBlockState, double[]> fn) {
        return (state, x, y, z, q, sink) -> ShapeCatalog.emit(fn.apply(state), x, y, z, sink);
    }

    private static void fixed(double[] boxes, StateType... types) {
        ShapeResolver resolver = fixed(boxes);
        for (StateType type : types) RESOLVERS.put(type, resolver);
    }

    private static void byState(Function<WrappedBlockState, double[]> fn, StateType... types) {
        ShapeResolver resolver = byState(fn);
        for (StateType type : types) RESOLVERS.put(type, resolver);
    }

    private static void tag(BlockTags tag, ShapeResolver resolver) {
        for (StateType type : tag.getStates()) RESOLVERS.put(type, resolver);
    }
}
