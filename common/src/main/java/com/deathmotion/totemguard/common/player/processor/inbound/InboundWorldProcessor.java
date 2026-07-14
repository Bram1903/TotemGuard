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

import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.DiggingData;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.BreakSpeed;
import com.deathmotion.totemguard.common.world.block.PredictedBlocks;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Part;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public class InboundWorldProcessor extends ProcessorInbound {

    private static final long PREDICTION_TIMEOUT_MILLIS = 1500;
    private static final int MAX_STARTS_PER_SECOND = 60;
    private static final int MIN_PLACEMENT_CHAIN = 3;
    private static final int MAX_PLACEMENT_CHAIN = 16;
    private static final double REACH_SLACK = 2.0;
    private static final double STANDING_EYE_HEIGHT = 1.62;
    private static final double SNEAKING_EYE_HEIGHT = 1.27;
    private static final double EYE_FLUID_OFFSET = 0.1111111;

    private final Data data;
    private final BlockReader reader;
    private final PredictedBlocks predicted;
    private final DiggingData digging;
    private final BreakSpeed.VersionRules breakRules;

    private PendingEdit pendingEdit;

    public InboundWorldProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.reader = player.getWorldMirror().reader();
        this.predicted = player.getWorldMirror().predicted();
        this.digging = player.getData().getDiggingData();

        VersionGates gates = player.getVersionGates();
        this.breakRules = new BreakSpeed.VersionRules(
                gates.blockBreakComponentEra(),
                gates.blockBreakAttributeEra(),
                gates.creativeDestroyComponentEra(),
                gates.maceCreativePenalty(),
                gates.harvestOverride1214(),
                gates.harvestOverride121());
    }

    private static boolean doublePlant(StateType type) {
        return type == StateTypes.TALL_GRASS || type == StateTypes.LARGE_FERN
                || type == StateTypes.SUNFLOWER || type == StateTypes.LILAC
                || type == StateTypes.ROSE_BUSH || type == StateTypes.PEONY
                || type == StateTypes.TALL_SEAGRASS || type == StateTypes.PITCHER_PLANT
                || type == StateTypes.SMALL_DRIPLEAF;
    }

    private static double axisDistance(double point, int cellMin) {
        if (point < cellMin) return cellMin - point;
        if (point > cellMin + 1) return point - (cellMin + 1);
        return 0.0;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        pendingEdit = null;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            predicted.expire(event.getTimestamp(), PREDICTION_TIMEOUT_MILLIS);
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            Vector3i position = packet.getBlockPosition();
            switch (packet.getAction()) {
                case START_DIGGING -> onStartDigging(position, packet.getSequence(), event.getTimestamp());
                case CANCELLED_DIGGING ->
                        digging.onAbort(position.getX(), position.getY(), position.getZ(), event.getTimestamp());
                case FINISHED_DIGGING -> onFinishDigging(position, packet.getSequence(), event.getTimestamp());
                default -> {
                }
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            predicted.expire(event.getTimestamp(), PREDICTION_TIMEOUT_MILLIS);
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            if (packet.getFace() != BlockFace.OTHER) {
                onPlacement(packet, event.getTimestamp());
            }
        } else if (packetType == PacketType.Play.Client.ANIMATION
                || WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            sampleDigging();
            predicted.expire(event.getTimestamp(), PREDICTION_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        PendingEdit edit = pendingEdit;
        pendingEdit = null;
        if (edit == null || event.isCancelled()) return;
        applyEdit(edit);
    }

    private void applyEdit(PendingEdit edit) {
        if (edit.place()) {
            predicted.predict(edit.x(), edit.y(), edit.z(), edit.serverStateId(),
                    edit.sequence(), edit.chainDepth(), edit.nowMillis());
            return;
        }
        if (!predicted.predict(edit.x(), edit.y(), edit.z(), 0, edit.sequence(), 0, edit.nowMillis())) return;
        if (edit.hasPartner()) {
            predicted.predict(edit.partnerX(), edit.partnerY(), edit.partnerZ(), 0, edit.sequence(), 0, edit.nowMillis());
        }
    }

    private void onStartDigging(Vector3i position, int sequence, long nowMillis) {
        if (data.getGameMode() == GameMode.SPECTATOR || data.isDead()) return;

        WrappedBlockState block = reader.state(position.getX(), position.getY(), position.getZ());
        float perTick = perTickProgress(block.getType());
        boolean instant = perTick >= 1.0f;
        digging.onStart(instant, position.getX(), position.getY(), position.getZ(),
                heldItemType(), perTick, nowMillis);

        boolean rateOk = data.getGameMode() == GameMode.CREATIVE || digging.startsInWindow() <= MAX_STARTS_PER_SECOND;
        if (instant && rateOk
                && data.getGameMode() != GameMode.ADVENTURE
                && breakableForEdit(block)
                && withinReach(position.getX(), position.getY(), position.getZ())) {
            pendingEdit = breakEdit(position.getX(), position.getY(), position.getZ(), block, sequence, nowMillis);
        }
    }

    private void onFinishDigging(Vector3i position, int sequence, long nowMillis) {
        if (data.getGameMode() == GameMode.SPECTATOR || data.isDead()) return;

        boolean plausibleTiming = digging.onFinish(position.getX(), position.getY(), position.getZ(), nowMillis);

        WrappedBlockState block = reader.state(position.getX(), position.getY(), position.getZ());
        if (plausibleTiming
                && data.getGameMode() != GameMode.ADVENTURE
                && breakableForEdit(block)
                && withinReach(position.getX(), position.getY(), position.getZ())) {
            pendingEdit = breakEdit(position.getX(), position.getY(), position.getZ(), block, sequence, nowMillis);
        }
    }

    private PendingEdit breakEdit(int x, int y, int z, WrappedBlockState block, int sequence, long nowMillis) {
        StateType type = block.getType();
        if (BlockTags.DOORS.contains(type) || doublePlant(type)) {
            int partnerY = block.hasProperty(StateValue.HALF) && block.getHalf() == Half.UPPER ? y - 1 : y + 1;
            return PendingEdit.breakTwo(x, y, z, x, partnerY, z, sequence, nowMillis);
        }
        if (BlockTags.BEDS.contains(type) && block.hasProperty(StateValue.PART)) {
            BlockFace facing = block.getFacing();
            int dir = block.getPart() == Part.FOOT ? 1 : -1;
            return PendingEdit.breakTwo(x, y, z,
                    x + facing.getModX() * dir, y, z + facing.getModZ() * dir, sequence, nowMillis);
        }
        return PendingEdit.breakOne(x, y, z, sequence, nowMillis);
    }

    private void onPlacement(WrapperPlayClientPlayerBlockPlacement packet, long nowMillis) {
        GameMode gameMode = data.getGameMode();
        if (gameMode == GameMode.SPECTATOR || data.isDead()) return;

        ItemStack held = packet.getHand() == InteractionHand.MAIN_HAND
                ? player.getInventory().getMainHandItem()
                : player.getInventory().getOffhandItem();
        StateType placedType = held == null ? null : held.getType().getPlacedType();
        if (placedType == null || placedType.isAir()) return;

        Vector3i clicked = packet.getBlockPosition();
        WrappedBlockState clickedState = reader.state(clicked.getX(), clicked.getY(), clicked.getZ());
        if (interactiveWithoutSneak(clickedState.getType()) && !data.isSneaking()) return;

        if (gameMode == GameMode.ADVENTURE) return;
        if (!withinReach(clicked.getX(), clicked.getY(), clicked.getZ())) return;

        BlockFace face = packet.getFace();
        boolean replaceClicked = replaceable(clicked.getX(), clicked.getY(), clicked.getZ());
        int targetX = replaceClicked ? clicked.getX() : clicked.getX() + face.getModX();
        int targetY = replaceClicked ? clicked.getY() : clicked.getY() + face.getModY();
        int targetZ = replaceClicked ? clicked.getZ() : clicked.getZ() + face.getModZ();

        if (!replaceClicked && !replaceable(targetX, targetY, targetZ)) return;

        int chainDepth = 0;
        if (!clickedAgainstConfirmed(clicked.getX(), clicked.getY(), clicked.getZ())) {
            if (!predictedNonAir(clicked.getX(), clicked.getY(), clicked.getZ())) return;
            chainDepth = predicted.chainDepth(clicked.getX(), clicked.getY(), clicked.getZ()) + 1;
            if (chainDepth > maxPlacementChain()) return;
        }

        ClientVersion serverBlockVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        WrappedBlockState placedState = WrappedBlockState.getDefaultState(serverBlockVersion, placedType);
        long placedFacts = reader.factsForClientId(reader.stateMap().toClientId(placedState.getGlobalId()));
        if (StateFacts.is(placedFacts, StateFacts.FULL_CUBE) && intersectsSelf(targetX, targetY, targetZ)) return;

        pendingEdit = PendingEdit.place(targetX, targetY, targetZ,
                placedState.getGlobalId(), packet.getSequence(), chainDepth, nowMillis);
    }

    private void sampleDigging() {
        if (!digging.hasTarget()) return;
        WrappedBlockState block = reader.state(digging.targetX(), digging.targetY(), digging.targetZ());
        if (block.getType().isAir()) return;
        digging.sample(heldItemType(), perTickProgress(block.getType()));
    }

    private ItemType heldItemType() {
        ItemStack held = player.getInventory().getMainHandItem();
        return held == null ? null : held.getType();
    }

    private float perTickProgress(StateType block) {
        return BreakSpeed.perTickProgress(new BreakSpeed.Query(
                breakRules,
                data.getGameMode(),
                player.getInventory().getMainHandItem(),
                block,
                data.getEffectData().hasHaste() ? data.getEffectData().hasteAmplifier() : -1,
                data.getEffectData().hasConduitPower() ? data.getEffectData().conduitPowerAmplifier() : -1,
                data.getEffectData().hasMiningFatigue() ? data.getEffectData().miningFatigueAmplifier() : -1,
                data.getAttributeData().blockBreakSpeed(),
                data.getAttributeData().miningEfficiency(),
                data.getAttributeData().submergedMiningSpeed(),
                aquaAffinity(),
                eyeInWater(),
                data.getMovementData().isOnGround()));
    }

    private boolean aquaAffinity() {
        for (int slot = InventoryConstants.SLOT_HELMET; slot <= InventoryConstants.SLOT_BOOTS; slot++) {
            ItemStack armor = player.getInventory().getItem(slot);
            if (armor != null && armor.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY) > 0) return true;
        }
        return false;
    }

    private boolean eyeInWater() {
        Location current = data.getMovementData().getCurrent();
        double scale = data.getAttributeData().scale();
        return eyeSampleInWater(current, STANDING_EYE_HEIGHT * scale)
                && eyeSampleInWater(current, SNEAKING_EYE_HEIGHT * scale);
    }

    private boolean eyeSampleInWater(Location at, double eyeHeight) {
        double sampleY = at.getY() + eyeHeight - EYE_FLUID_OFFSET;
        int x = floor(at.getX());
        int y = floor(sampleY);
        int z = floor(at.getZ());
        if (reader.uncertain(x, y, z)) return false;
        long facts = reader.facts(x, y, z);
        if (!StateFacts.is(facts, StateFacts.WATER)) return false;
        return sampleY <= y + StateFacts.fluidHeight(facts);
    }

    private boolean breakableForEdit(WrappedBlockState block) {
        StateType type = block.getType();
        if (type.isAir()) return false;
        if (type == StateTypes.WATER || type == StateTypes.LAVA) return false;
        return data.getGameMode() == GameMode.CREATIVE || type.getHardness() != -1.0f;
    }

    private int maxPlacementChain() {
        int ping = player.getPingData().getTransactionPing();
        if (ping <= 0) return MIN_PLACEMENT_CHAIN;
        int scaled = (int) Math.ceil(ping / 50.0) + MIN_PLACEMENT_CHAIN;
        return Math.min(MAX_PLACEMENT_CHAIN, Math.max(MIN_PLACEMENT_CHAIN, scaled));
    }

    private boolean withinReach(int x, int y, int z) {
        double reach = data.getAttributeData().blockInteractionRange() + REACH_SLACK;
        if (data.getGameMode() == GameMode.CREATIVE) reach += 0.5;
        double reachSquared = reach * reach;
        return eyeDistanceSquared(data.getMovementData().getCurrent(), x, y, z) <= reachSquared
                || eyeDistanceSquared(data.getMovementData().getPrevious(), x, y, z) <= reachSquared;
    }

    private double eyeDistanceSquared(Location at, int x, int y, int z) {
        double eyeY = at.getY() + STANDING_EYE_HEIGHT * data.getAttributeData().scale();
        double dx = axisDistance(at.getX(), x);
        double dy = axisDistance(eyeY, y);
        double dz = axisDistance(at.getZ(), z);
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean replaceable(int x, int y, int z) {
        return replaceableId(reader.stateId(x, y, z))
                || replaceableId(reader.pendingStateId(x, y, z));
    }

    private boolean replaceableId(int clientId) {
        if (clientId < 0) return false;
        if (clientId == 0) return true;
        long facts = reader.factsForClientId(clientId);
        if (StateFacts.is(facts, StateFacts.AIR)) return true;
        if (StateFacts.is(facts, StateFacts.ANY_FLUID) && !StateFacts.is(facts, StateFacts.HAS_SHAPE)) return true;
        return reader.stateForClientId(clientId).getType().isReplaceable();
    }

    private boolean clickedAgainstConfirmed(int x, int y, int z) {
        return solidAnchorId(reader.stateId(x, y, z)) || solidAnchorId(reader.pendingStateId(x, y, z));
    }

    private boolean predictedNonAir(int x, int y, int z) {
        return solidAnchorId(reader.predictedStateId(x, y, z));
    }

    private boolean solidAnchorId(int clientId) {
        if (clientId <= 0) return false;
        long facts = reader.factsForClientId(clientId);
        if (StateFacts.is(facts, StateFacts.AIR)) return false;
        StateType type = reader.stateForClientId(clientId).getType();
        return type != StateTypes.WATER && type != StateTypes.LAVA;
    }

    private boolean intersectsSelf(int x, int y, int z) {
        Location at = data.getMovementData().getCurrent();
        double halfWidth = data.getAttributeData().width() / 2.0;
        double height = data.getAttributeData().height();
        return at.getX() + halfWidth > x && at.getX() - halfWidth < x + 1
                && at.getY() + height > y && at.getY() < y + 1
                && at.getZ() + halfWidth > z && at.getZ() - halfWidth < z + 1;
    }

    private boolean interactiveWithoutSneak(StateType type) {
        return type == StateTypes.CHEST || type == StateTypes.TRAPPED_CHEST || type == StateTypes.ENDER_CHEST
                || type == StateTypes.BARREL || type == StateTypes.CRAFTING_TABLE || type == StateTypes.FURNACE
                || type == StateTypes.BLAST_FURNACE || type == StateTypes.SMOKER || type == StateTypes.DISPENSER
                || type == StateTypes.DROPPER || type == StateTypes.HOPPER || type == StateTypes.BREWING_STAND
                || type == StateTypes.ENCHANTING_TABLE || type == StateTypes.GRINDSTONE || type == StateTypes.STONECUTTER
                || type == StateTypes.LOOM || type == StateTypes.CARTOGRAPHY_TABLE || type == StateTypes.SMITHING_TABLE
                || type == StateTypes.LECTERN || type == StateTypes.BELL || type == StateTypes.LEVER
                || type == StateTypes.REPEATER || type == StateTypes.COMPARATOR || type == StateTypes.NOTE_BLOCK
                || type == StateTypes.CRAFTER
                || BlockTags.DOORS.contains(type) || BlockTags.TRAPDOORS.contains(type)
                || BlockTags.FENCE_GATES.contains(type) || BlockTags.BUTTONS.contains(type)
                || BlockTags.BEDS.contains(type) || BlockTags.ANVIL.contains(type)
                || BlockTags.SHULKER_BOXES.contains(type);
    }

    private record PendingEdit(boolean place, int x, int y, int z, int serverStateId,
                               int sequence, int chainDepth, long nowMillis,
                               boolean hasPartner, int partnerX, int partnerY, int partnerZ) {

        static PendingEdit place(int x, int y, int z, int serverStateId, int sequence, int chainDepth, long nowMillis) {
            return new PendingEdit(true, x, y, z, serverStateId, sequence, chainDepth, nowMillis, false, 0, 0, 0);
        }

        static PendingEdit breakOne(int x, int y, int z, int sequence, long nowMillis) {
            return new PendingEdit(false, x, y, z, 0, sequence, 0, nowMillis, false, 0, 0, 0);
        }

        static PendingEdit breakTwo(int x, int y, int z, int partnerX, int partnerY, int partnerZ, int sequence, long nowMillis) {
            return new PendingEdit(false, x, y, z, 0, sequence, 0, nowMillis, true, partnerX, partnerY, partnerZ);
        }
    }
}
