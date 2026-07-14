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

package com.deathmotion.totemguard.common.world.block;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemTool;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

public final class BreakSpeed {

    private static final Set<StateType> HARVESTABLE_TYPES_1_21_4 = Set.of(
            StateTypes.BELL,
            StateTypes.LANTERN,
            StateTypes.SOUL_LANTERN,
            StateTypes.COPPER_DOOR,
            StateTypes.EXPOSED_COPPER_DOOR,
            StateTypes.OXIDIZED_COPPER_DOOR,
            StateTypes.WEATHERED_COPPER_DOOR,
            StateTypes.WAXED_COPPER_DOOR,
            StateTypes.WAXED_EXPOSED_COPPER_DOOR,
            StateTypes.WAXED_OXIDIZED_COPPER_DOOR,
            StateTypes.WAXED_WEATHERED_COPPER_DOOR,
            StateTypes.IRON_DOOR,
            StateTypes.HEAVY_WEIGHTED_PRESSURE_PLATE,
            StateTypes.LIGHT_WEIGHTED_PRESSURE_PLATE,
            StateTypes.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            StateTypes.STONE_PRESSURE_PLATE,
            StateTypes.BREWING_STAND,
            StateTypes.ENDER_CHEST
    );

    private static final Set<StateType> HARVESTABLE_TYPES_1_21 = Set.of(StateTypes.VAULT);

    private BreakSpeed() {
    }

    public static float perTickProgress(Query query) {
        VersionRules rules = query.rules;
        ItemStack heldItem = query.heldItem == null ? ItemStack.EMPTY : query.heldItem;
        ItemType toolType = heldItem.getType();

        if (query.gameMode == GameMode.CREATIVE) {
            if (rules.creativeDestroyComponentEra) {
                return heldItem.getComponent(ComponentTypes.TOOL)
                        .map(ItemTool::isCanDestroyBlocksInCreative)
                        .orElse(true) ? 1.0f : 0.0f;
            }
            if (toolType.hasAttribute(ItemTypes.ItemAttribute.SWORD) || toolType == ItemTypes.TRIDENT
                    || toolType == ItemTypes.DEBUG_STICK
                    || (toolType == ItemTypes.MACE && rules.maceCreativePenalty)) {
                return 0.0f;
            }
            return 1.0f;
        }

        float hardness = query.block.getHardness();
        if (hardness == -1.0f) return 0.0f;

        ToolSpeedData toolData = rules.componentEra
                ? modernToolSpeedData(heldItem, query.block)
                : legacyToolSpeedData(heldItem, query.block);

        float speed = speedMultiplier(query, heldItem, toolData);

        boolean canHarvest = !query.block.isRequiresCorrectTool() || toolData.correctToolForDrops
                || (rules.harvestOverride1214 && HARVESTABLE_TYPES_1_21_4.contains(query.block))
                || (rules.harvestOverride121 && HARVESTABLE_TYPES_1_21.contains(query.block));

        float progress = speed / hardness;
        progress /= canHarvest ? 30.0f : 100.0f;
        return progress;
    }

    private static float speedMultiplier(Query query, ItemStack heldItem, ToolSpeedData toolData) {
        float speed = toolData.speedMultiplier;

        if (speed > 1.0f) {
            if (query.rules.attributeEfficiencyEra) {
                speed += (float) query.miningEfficiencyAttribute;
            } else {
                int efficiency = heldItem.getEnchantmentLevel(EnchantmentTypes.BLOCK_EFFICIENCY);
                if (efficiency > 0) speed += efficiency * efficiency + 1;
            }
        }

        if (query.hasteAmplifier >= 0 || query.conduitPowerAmplifier >= 0) {
            int hasteLevel = Math.max(Math.max(query.hasteAmplifier, 0), Math.max(query.conduitPowerAmplifier, 0));
            speed *= (float) (1 + (0.2 * (hasteLevel + 1)));
        }

        if (query.miningFatigueAmplifier >= 0) {
            speed *= switch (query.miningFatigueAmplifier) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            };
        }

        speed *= (float) query.blockBreakSpeedAttribute;

        if (query.eyeInWater) {
            if (query.rules.attributeEfficiencyEra) {
                speed *= (float) query.submergedMiningSpeedAttribute;
            } else if (!query.aquaAffinity) {
                speed /= 5.0f;
            }
        }

        if (!query.onGround) {
            speed /= 5.0f;
        }

        return speed;
    }

    private static ToolSpeedData modernToolSpeedData(ItemStack heldItem, StateType block) {
        Optional<ItemTool> component = heldItem.getComponent(ComponentTypes.TOOL);
        if (component.isEmpty()) return new ToolSpeedData(1.0f, false);

        ItemTool itemTool = component.get();
        float speed = itemTool.getDefaultMiningSpeed();
        boolean correctForDrops = false;
        boolean speedFound = false;
        boolean dropsFound = false;

        for (ItemTool.Rule rule : itemTool.getRules()) {
            MappedEntitySet<StateType.Mapped> blocks = rule.getBlocks();
            ResourceLocation tagKey = blocks.getTagKey();
            boolean matches;
            if (tagKey != null) {
                BlockTags tag = BlockTags.getByName(tagKey.getKey());
                matches = tag != null && tag.contains(block);
            } else {
                matches = blocks.getEntities().contains(block.getMapped());
            }
            if (!matches) continue;

            if (!speedFound && rule.getSpeed() != null) {
                speed = rule.getSpeed();
                speedFound = true;
            }
            if (!dropsFound && rule.getCorrectForDrops() != null) {
                correctForDrops = rule.getCorrectForDrops();
                dropsFound = true;
            }
            if (speedFound && dropsFound) break;
        }
        return new ToolSpeedData(speed, correctForDrops);
    }

    private static ToolSpeedData legacyToolSpeedData(ItemStack heldItem, StateType block) {
        ItemType toolType = heldItem.getType();
        float speed = 1.0f;
        boolean correctForDrops = false;

        if (toolType.hasAttribute(ItemTypes.ItemAttribute.AXE)) {
            correctForDrops = BlockTags.MINEABLE_AXE.contains(block);
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.PICKAXE)) {
            correctForDrops = BlockTags.MINEABLE_PICKAXE.contains(block);
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.SHOVEL)) {
            correctForDrops = BlockTags.MINEABLE_SHOVEL.contains(block);
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.HOE)) {
            correctForDrops = BlockTags.MINEABLE_HOE.contains(block);
        }

        if (correctForDrops) {
            int tier = 0;
            if (toolType.hasAttribute(ItemTypes.ItemAttribute.WOOD_TIER)) {
                speed = 2.0f;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.STONE_TIER)) {
                speed = 4.0f;
                tier = 1;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.IRON_TIER)) {
                speed = 6.0f;
                tier = 2;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.DIAMOND_TIER)) {
                speed = 8.0f;
                tier = 3;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.GOLD_TIER)) {
                speed = 12.0f;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.NETHERITE_TIER)) {
                speed = 9.0f;
                tier = 4;
            }

            if (tier < 3 && BlockTags.NEEDS_DIAMOND_TOOL.contains(block)) {
                correctForDrops = false;
            } else if (tier < 2 && BlockTags.NEEDS_IRON_TOOL.contains(block)) {
                correctForDrops = false;
            } else if (tier < 1 && BlockTags.NEEDS_STONE_TOOL.contains(block)) {
                correctForDrops = false;
            }
        }

        if (toolType == ItemTypes.SHEARS) {
            if (block == StateTypes.COBWEB || BlockTags.LEAVES.contains(block)) {
                speed = 15.0f;
            } else if (BlockTags.WOOL.contains(block)) {
                speed = 5.0f;
            } else if (block == StateTypes.VINE || block == StateTypes.GLOW_LICHEN) {
                speed = 2.0f;
            }
            correctForDrops = block == StateTypes.COBWEB
                    || block == StateTypes.REDSTONE_WIRE
                    || block == StateTypes.TRIPWIRE;
        }

        if (toolType.hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
            if (block == StateTypes.COBWEB) {
                speed = 15.0f;
            } else if (BlockTags.SWORD_EFFICIENT.contains(block)) {
                speed = 1.5f;
            }
            correctForDrops = block == StateTypes.COBWEB;
        }

        return new ToolSpeedData(speed, correctForDrops);
    }

    public record VersionRules(
            boolean componentEra,
            boolean attributeEfficiencyEra,
            boolean creativeDestroyComponentEra,
            boolean maceCreativePenalty,
            boolean harvestOverride1214,
            boolean harvestOverride121) {
    }

    public record Query(
            VersionRules rules,
            GameMode gameMode,
            ItemStack heldItem,
            StateType block,
            int hasteAmplifier,
            int conduitPowerAmplifier,
            int miningFatigueAmplifier,
            double blockBreakSpeedAttribute,
            double miningEfficiencyAttribute,
            double submergedMiningSpeedAttribute,
            boolean aquaAffinity,
            boolean eyeInWater,
            boolean onGround) {
    }

    private record ToolSpeedData(float speedMultiplier, boolean correctToolForDrops) {
    }
}
