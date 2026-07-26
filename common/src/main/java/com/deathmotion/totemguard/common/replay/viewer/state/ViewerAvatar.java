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

package com.deathmotion.totemguard.common.replay.viewer.state;

import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.replay.viewer.net.ViewerSink;
import com.deathmotion.totemguard.common.util.MetadataIndex;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ViewerAvatar {

    public static final int ENTITY_ID = Integer.MAX_VALUE - 16;

    private static final byte FLAG_SNEAKING = 0x02;
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_SPRINTING = 0x08;
    private static final byte FLAG_GLIDING = (byte) 0x80;
    private static final byte ALL_SKIN_LAYERS = 0x7F;
    private static final double STANDING_EYE_HEIGHT = 1.62;
    private static final double SNEAKING_EYE_HEIGHT = 1.27;

    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.BOOTS, EquipmentSlot.LEGGINGS, EquipmentSlot.CHEST_PLATE, EquipmentSlot.HELMET
    };
    private static final int[] ARMOUR_SLOTS = {
            InventoryConstants.SLOT_BOOTS, InventoryConstants.SLOT_LEGGINGS,
            InventoryConstants.SLOT_CHESTPLATE, InventoryConstants.SLOT_HELMET
    };

    private final ViewerSink sink;
    private final ServerVersion server;
    private final MetadataIndex metadata;
    private final UUID uuid;
    private final String name;
    private final ItemStack[] inventory = new ItemStack[InventoryConstants.INVENTORY_SIZE];

    private UserProfile profile;
    private int heldSlot;

    private boolean spawned;
    @Getter
    private boolean positioned;
    @Getter
    private double x;
    @Getter
    private double y;
    @Getter
    private double z;
    @Getter
    private float yaw;
    @Getter
    private float pitch;
    private boolean onGround;
    private boolean moved;

    private boolean sneaking;
    private boolean sprinting;
    private boolean gliding;
    private boolean hidden;

    public ViewerAvatar(ViewerSink sink, ServerVersion server, MetadataIndex metadata,
                        UUID recordedUuid, String recordedName) {
        this.sink = sink;
        this.server = server;
        this.metadata = metadata;
        this.uuid = derive(recordedUuid);
        this.name = recordedName;
        this.profile = new UserProfile(uuid, recordedName);
        Arrays.fill(inventory, ItemStack.EMPTY);
    }

    private static UUID derive(UUID recorded) {
        return UUID.nameUUIDFromBytes(("TotemGuardReplayAvatar:" + recorded)
                .getBytes(StandardCharsets.UTF_8));
    }

    public void adoptTextures(UserProfile recorded) {
        if (recorded.getTextureProperties().isEmpty()) return;
        if (!profile.getTextureProperties().isEmpty()) return;

        UserProfile adopted = new UserProfile(uuid, name);
        adopted.setTextureProperties(new ArrayList<>(recorded.getTextureProperties()));
        this.profile = adopted;
        if (!spawned) return;
        despawn();
        spawn();
    }

    public void moveTo(double x, double y, double z, boolean onGround) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
        if (onGround && gliding) gliding = false;
        boolean first = !positioned;
        this.positioned = true;
        if (first) {
            spawn();
            return;
        }
        this.moved = true;
    }

    public void rotateTo(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.moved = true;
    }

    public double eyeY() {
        return y + (sneaking ? SNEAKING_EYE_HEIGHT : STANDING_EYE_HEIGHT);
    }

    public void hidden(boolean value) {
        if (hidden == value) return;
        hidden = value;
        if (spawned) sendPose();
    }

    public void flush() {
        if (!moved || !spawned) return;
        moved = false;
        sendPlacement();
    }

    public void sneaking(boolean value) {
        if (sneaking == value) return;
        sneaking = value;
        if (spawned) sendPose();
    }

    public void sprinting(boolean value) {
        if (sprinting == value) return;
        sprinting = value;
        if (spawned) sendPose();
    }

    public void gliding(boolean value) {
        if (gliding == value) return;
        gliding = value;
        if (spawned) sendPose();
    }

    public void swing() {
        if (!spawned) return;
        sink.send(new WrapperPlayServerEntityAnimation(ENTITY_ID,
                WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM));
    }

    public void heldSlot(int slot) {
        if (slot < 0 || slot > 8 || slot == heldSlot) return;
        this.heldSlot = slot;
        if (spawned) sendEquipment();
    }

    public void inventorySlot(int slot, @Nullable ItemStack item) {
        if (slot < 0 || slot >= inventory.length) return;
        inventory[slot] = item == null ? ItemStack.EMPTY : item;
        if (spawned) sendEquipment();
    }

    public void inventory(List<ItemStack> items) {
        int limit = Math.min(items.size(), inventory.length);
        Arrays.fill(inventory, ItemStack.EMPTY);
        for (int slot = 0; slot < limit; slot++) {
            ItemStack item = items.get(slot);
            inventory[slot] = item == null ? ItemStack.EMPTY : item;
        }
        if (spawned) sendEquipment();
    }

    public void resend() {
        spawned = false;
        moved = false;
        if (positioned) spawn();
    }

    public void forget() {
        spawned = false;
        positioned = false;
        moved = false;
        sneaking = false;
        sprinting = false;
        gliding = false;
        onGround = false;
        heldSlot = 0;
        Arrays.fill(inventory, ItemStack.EMPTY);
    }

    public void despawn() {
        if (!spawned) return;
        spawned = false;
        sink.send(new WrapperPlayServerDestroyEntities(ENTITY_ID));
        if (server.isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
            sink.send(new WrapperPlayServerPlayerInfoRemove(List.of(uuid)));
            return;
        }
        sink.send(new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                new WrapperPlayServerPlayerInfo.PlayerData(null, profile, null, 0)));
    }

    private void spawn() {
        if (!positioned) return;
        sendListing();
        if (server.isNewerThanOrEquals(ServerVersion.V_1_20_2)) {
            sink.send(new WrapperPlayServerSpawnEntity(ENTITY_ID, Optional.of(uuid), EntityTypes.PLAYER,
                    new Vector3d(x, y, z), pitch, yaw, yaw, 0, Optional.empty()));
        } else {
            sink.send(new WrapperPlayServerSpawnPlayer(ENTITY_ID, uuid,
                    new Vector3d(x, y, z), yaw, pitch, List.of()));
        }
        spawned = true;
        sink.send(new WrapperPlayServerEntityHeadLook(ENTITY_ID, yaw));
        sendPose();
        sendEquipment();
    }

    private void sendPlacement() {
        sink.send(new WrapperPlayServerEntityTeleport(ENTITY_ID, new Vector3d(x, y, z), yaw, pitch, onGround));
        sink.send(new WrapperPlayServerEntityHeadLook(ENTITY_ID, yaw));
    }

    private void sendListing() {
        if (server.isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry =
                    new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile, false, 0,
                            GameMode.SURVIVAL, null, null);
            sink.send(new WrapperPlayServerPlayerInfoUpdate(
                    EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                    entry));
            return;
        }
        sink.send(new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                new WrapperPlayServerPlayerInfo.PlayerData(null, profile, GameMode.SURVIVAL, 0)));
    }

    private void sendPose() {
        byte flags = 0;
        if (sneaking) flags |= FLAG_SNEAKING;
        if (sprinting) flags |= FLAG_SPRINTING;
        if (gliding) flags |= FLAG_GLIDING;
        if (hidden) flags |= FLAG_INVISIBLE;

        EntityPose pose = gliding ? EntityPose.FALL_FLYING
                : sneaking ? EntityPose.CROUCHING
                : EntityPose.STANDING;

        List<EntityData<?>> data = new ArrayList<>(3);
        data.add(new EntityData<>(0, EntityDataTypes.BYTE, flags));
        data.add(new EntityData<>(metadata.pose(), EntityDataTypes.ENTITY_POSE, pose));
        data.add(new EntityData<>(metadata.skinParts(), EntityDataTypes.BYTE, ALL_SKIN_LAYERS));
        sink.send(new WrapperPlayServerEntityMetadata(ENTITY_ID, data));
    }

    private void sendEquipment() {
        List<Equipment> equipment = new ArrayList<>(6);
        equipment.add(new Equipment(EquipmentSlot.MAIN_HAND,
                inventory[InventoryConstants.HOTBAR_START + heldSlot]));
        equipment.add(new Equipment(EquipmentSlot.OFF_HAND, inventory[InventoryConstants.SLOT_OFFHAND]));
        for (int i = 0; i < ARMOUR.length; i++) {
            equipment.add(new Equipment(ARMOUR[i], inventory[ARMOUR_SLOTS[i]]));
        }
        sink.send(new WrapperPlayServerEntityEquipment(ENTITY_ID, equipment));
    }
}
