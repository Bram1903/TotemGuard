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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.*;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemUseEffects;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.*;

import java.util.Optional;

public class InboundActionProcessor extends ProcessorInbound {

    private static final double DEFAULT_USE_SLOWDOWN = 0.2F;

    private final Data data;
    private final ClickData clickData;
    private final TickData tickData;
    private final InputData inputData;
    private final CombatTracker combatTracker;

    public InboundActionProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.clickData = player.getClickData();
        this.tickData = player.getTickData();
        this.inputData = player.getData().getInputData();
        this.combatTracker = player.getCombatTracker();
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.ANIMATION) {
            if (tickData.isInvalidLeftClick()) return;
            clickData.recordLeftClick();
        } else if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            if (new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER) {
                tickData.setUsing(true);
            } else {
                tickData.setPlacing(true);
            }

            clickData.recordRightClick();
        } else if (packetType == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                tickData.setAttacking(true);
                combatTracker.recordOutgoingAttack(packet.getEntityId(), event.getTimestamp());
            } else {
                tickData.setInteracting(true);
            }
        } else if (packetType == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            tickData.setAttacking(true);
            combatTracker.recordOutgoingAttack(packet.getEntityId(), event.getTimestamp());
        } else if (packetType == PacketType.Play.Client.USE_ITEM) {
            tickData.setUsing(true);
            clickData.recordRightClick();
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
            boolean mainHand = packet.getHand() == InteractionHand.MAIN_HAND;
            ItemStack used = mainHand
                    ? player.getInventory().getMainHandItem()
                    : player.getInventory().getOffhandItem();
            data.getUseItemData().onUseItem(mainHand, useSlowdownMultiplier(used));
        } else if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getEntityId() != player.getUser().getEntityId()) return;

            switch (packet.getAction()) {
                case START_SNEAKING -> {
                    data.setSneaking(true);
                    tickData.setSneaking(true);
                }
                case STOP_SNEAKING -> {
                    data.setSneaking(false);
                    tickData.setSneaking(true);
                }
                case START_SPRINTING -> {
                    data.setSprinting(true);
                    data.recordSprinting(true);
                    tickData.setSprinting(true);
                }
                case STOP_SPRINTING -> {
                    data.setSprinting(false);
                    data.recordSprinting(false);
                    tickData.setSprinting(true);
                }
                case LEAVE_BED -> tickData.setLeavingBed(true);
                case START_JUMPING_WITH_HORSE -> {
                    tickData.setJumpingWithMount(true);
                    data.getVehicleData().onJumpClaim(packet.getJumpBoost());
                }
                case STOP_JUMPING_WITH_HORSE -> tickData.setJumpingWithMount(true);
                case START_FLYING_WITH_ELYTRA -> {
                    tickData.setStartingToGlide(true);
                    if (glideClaimLegal()) data.getGlideData().armClaim();
                }
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            switch (packet.getAction()) {
                case SWAP_ITEM_WITH_OFFHAND -> {
                    tickData.setSwapping(true);
                    data.getUseItemData().onSwap();
                }
                case DROP_ITEM, DROP_ITEM_STACK -> tickData.setDropping(true);
                case RELEASE_USE_ITEM -> {
                    tickData.setReleasing(true);
                    data.getUseItemData().onRelease();
                    armRiptideIfHeld();
                }
                case FINISHED_DIGGING, CANCELLED_DIGGING, START_DIGGING -> tickData.setDigging(true);
                case STAB -> {
                    tickData.setStabbing(true);
                    combatTracker.recordOutgoingStab(event.getTimestamp());
                }
            }
        } else if (packetType == PacketType.Play.Client.PICK_ITEM) {
            tickData.setPicking(true);
        } else if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            tickData.setClickingInInventory(true);

            switch (new WrapperPlayClientClickWindow(event).getWindowClickType()) {
                case QUICK_MOVE -> tickData.setQuickMoveClicking(true);
                case PICKUP, PICKUP_ALL -> tickData.setPickUpClicking(true);
            }
        } else if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            tickData.setClosingInventory(true);
        } else if (packetType == PacketType.Play.Client.PLAYER_INPUT && player.supportsEndTick()) {
            WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);
            inputData.setState(
                    packet.isForward(),
                    packet.isBackward(),
                    packet.isLeft(),
                    packet.isRight(),
                    packet.isJump(),
                    packet.isShift(),
                    packet.isSprint()
            );
            data.setSneaking(packet.isShift());
        } else if (packetType == PacketType.Play.Client.PLAYER_ABILITIES) {
            WrapperPlayClientPlayerAbilities packet = new WrapperPlayClientPlayerAbilities(event);
            data.setFlying(packet.isFlying() && data.isCanFly());
        }
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        clickData.checkPost();

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick()) {
            tickData.reset();
        }
    }

    private void armRiptideIfHeld() {
        int level = riptideLevel(player.getInventory().getMainHandItem());
        if (level <= 0) level = riptideLevel(player.getInventory().getOffhandItem());
        if (level <= 0) return;
        data.getGlideData().armRiptide(0.75 * (level + 1), data.getMovementData().isOnGround());
    }

    private static int riptideLevel(ItemStack stack) {
        if (stack == null || stack.getType() != ItemTypes.TRIDENT) return 0;
        return stack.getEnchantmentLevel(EnchantmentTypes.RIPTIDE);
    }

    private boolean glideClaimLegal() {
        if (data.isGliding()) return false;
        if (data.isInVehicle() || data.getEffectData().hasLevitation()) return false;
        if (usableGlider(player.getInventory().getItem(InventoryConstants.SLOT_CHESTPLATE))) return true;
        if (!player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)) return false;
        return usableGlider(player.getInventory().getItem(InventoryConstants.SLOT_HELMET))
                || usableGlider(player.getInventory().getItem(InventoryConstants.SLOT_LEGGINGS))
                || usableGlider(player.getInventory().getItem(InventoryConstants.SLOT_BOOTS));
    }

    private static boolean usableGlider(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getType() != ItemTypes.ELYTRA && stack.getComponent(ComponentTypes.GLIDER).isEmpty()) {
            return false;
        }
        return stack.getMaxDamage() <= 0 || stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    private double useSlowdownMultiplier(ItemStack used) {
        boolean componentEra = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        if (!componentEra) return DEFAULT_USE_SLOWDOWN;
        if (used == null) return 1.0;
        Optional<ItemUseEffects> effects = used.getComponent(ComponentTypes.USE_EFFECTS);
        if (effects.isPresent()) return effects.get().getSpeedMultiplier();
        return spear(used.getType()) ? 1.0 : DEFAULT_USE_SLOWDOWN;
    }

    private static boolean spear(ItemType type) {
        return type == ItemTypes.WOODEN_SPEAR
                || type == ItemTypes.STONE_SPEAR
                || type == ItemTypes.COPPER_SPEAR
                || type == ItemTypes.IRON_SPEAR
                || type == ItemTypes.GOLDEN_SPEAR
                || type == ItemTypes.DIAMOND_SPEAR
                || type == ItemTypes.NETHERITE_SPEAR;
    }
}
