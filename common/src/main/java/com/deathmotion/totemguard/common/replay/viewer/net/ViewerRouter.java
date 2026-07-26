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

package com.deathmotion.totemguard.common.replay.viewer.net;

import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.retention.BlockJournal;
import com.deathmotion.totemguard.common.replay.viewer.net.ViewerEntityIds.Shape;
import com.deathmotion.totemguard.common.replay.viewer.state.*;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class ViewerRouter {

    private static final Set<PacketTypeCommon> SELF_ONLY = Set.of(
            PacketType.Play.Server.UPDATE_HEALTH,
            PacketType.Play.Server.SET_EXPERIENCE,
            PacketType.Play.Server.PLAYER_ABILITIES,
            PacketType.Play.Server.CHANGE_GAME_STATE,
            PacketType.Play.Server.CAMERA,
            PacketType.Play.Server.OPEN_WINDOW,
            PacketType.Play.Server.OPEN_HORSE_WINDOW,
            PacketType.Play.Server.CLOSE_WINDOW,
            PacketType.Play.Server.SET_CURSOR_ITEM,
            PacketType.Play.Server.ACKNOWLEDGE_BLOCK_CHANGES,
            PacketType.Play.Server.PING,
            PacketType.Play.Server.KEEP_ALIVE,
            PacketType.Play.Server.CONFIGURATION_START
    );

    private static final Map<PacketTypeCommon, Shape> ENTITY_SHAPES = entityShapes();
    private static final int FALLBACK_VIEW_DISTANCE = 10;
    private static final int NO_CENTRE = Integer.MIN_VALUE;

    private final ViewerSink sink;
    private final ViewerWorld world;
    private final ViewerAvatar avatar;
    private final ViewerEntityIds ids;
    private final User scratch;
    private final Object channel = new Object();
    private final ServerVersion server;
    private final ClientVersion wire;
    private final UUID recordedUuid;
    private final UUID viewerUuid;
    private final Consumer<RecordedWorld> onWorld;

    @Getter
    private int recordedSelfId = -1;

    private int viewDistance = FALLBACK_VIEW_DISTANCE;
    private int centreX = NO_CENTRE;
    private int centreZ = NO_CENTRE;

    private volatile @Nullable ChunkLight light;
    private @Nullable Map<Long, ColumnMirror> mirror;

    public ViewerRouter(ViewerSink sink, ViewerWorld world, ViewerAvatar avatar, ViewerEntityIds ids,
                        User scratch, ServerVersion server, UUID recordedUuid, UUID viewerUuid,
                        Consumer<RecordedWorld> onWorld) {
        this.sink = sink;
        this.world = world;
        this.avatar = avatar;
        this.ids = ids;
        this.scratch = scratch;
        this.server = server;
        this.wire = server.toClientVersion();
        this.recordedUuid = recordedUuid;
        this.viewerUuid = viewerUuid;
        this.onWorld = onWorld;
    }

    private static Map<PacketTypeCommon, Shape> entityShapes() {
        Map<PacketTypeCommon, Shape> shapes = new HashMap<>();
        for (PacketTypeCommon type : List.of(
                PacketType.Play.Server.ENTITY_RELATIVE_MOVE,
                PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION,
                PacketType.Play.Server.ENTITY_VELOCITY,
                PacketType.Play.Server.ENTITY_TELEPORT,
                PacketType.Play.Server.ENTITY_POSITION_SYNC,
                PacketType.Play.Server.ENTITY_METADATA,
                PacketType.Play.Server.ENTITY_EQUIPMENT,
                PacketType.Play.Server.ENTITY_EFFECT,
                PacketType.Play.Server.REMOVE_ENTITY_EFFECT,
                PacketType.Play.Server.UPDATE_ATTRIBUTES,
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.SPAWN_LIVING_ENTITY,
                PacketType.Play.Server.SPAWN_PLAYER)) {
            shapes.put(type, Shape.LEADING_VARINT);
        }
        shapes.put(PacketType.Play.Server.ENTITY_STATUS, Shape.LEADING_INT);
        shapes.put(PacketType.Play.Server.DESTROY_ENTITIES, Shape.VARINT_LIST);
        shapes.put(PacketType.Play.Server.SET_PASSENGERS, Shape.VEHICLE_AND_LIST);
        return Map.copyOf(shapes);
    }

    private static int leadingVarInt(byte[] payload) {
        int value = 0;
        for (int i = 0, shift = 0; i < payload.length && shift <= 28; i++, shift += 7) {
            int b = payload[i] & 0xFF;
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
        }
        return -1;
    }

    public void light(ChunkLight light) {
        this.light = light;
    }

    public void route(RecordingFrame.Packet frame) {
        if (frame.state() != ConnectionState.PLAY) return;
        if (frame.inbound()) {
            inbound(frame);
            return;
        }
        outbound(frame);
    }

    private void inbound(RecordingFrame.Packet frame) {
        PacketTypeCommon type = PacketType.Play.Client.getById(wire, frame.packetId());
        if (type == null) return;

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            parseIn(frame, event -> {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                if (flying.hasRotationChanged()) {
                    avatar.rotateTo(flying.getLocation().getYaw(), flying.getLocation().getPitch());
                }
                if (flying.hasPositionChanged()) {
                    Vector3d position = flying.getLocation().getPosition();
                    follow(position.getX(), position.getZ());
                    avatar.moveTo(position.getX(), position.getY(), position.getZ(),
                            flying.isOnGround());
                }
            });
            return;
        }

        if (type == PacketType.Play.Client.ENTITY_ACTION) {
            parseIn(frame, event -> action(new WrapperPlayClientEntityAction(event)));
            return;
        }

        if (type == PacketType.Play.Client.ANIMATION) {
            avatar.swing();
            return;
        }

        if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            parseIn(frame, event -> avatar.heldSlot(new WrapperPlayClientHeldItemChange(event).getSlot()));
        }
    }

    private void action(WrapperPlayClientEntityAction packet) {
        switch (packet.getAction()) {
            case START_SNEAKING -> avatar.sneaking(true);
            case STOP_SNEAKING -> avatar.sneaking(false);
            case START_SPRINTING -> avatar.sprinting(true);
            case STOP_SPRINTING -> avatar.sprinting(false);
            case START_FLYING_WITH_ELYTRA -> avatar.gliding(true);
            default -> {
            }
        }
    }

    private void outbound(RecordingFrame.Packet frame) {
        PacketTypeCommon type = PacketType.Play.Server.getById(wire, frame.packetId());
        if (type == null || SELF_ONLY.contains(type)) return;

        if (type == PacketType.Play.Server.JOIN_GAME) {
            parseOut(frame, event -> {
                WrapperPlayServerJoinGame join = new WrapperPlayServerJoinGame(event);
                recordedSelfId = join.getEntityId();
                viewDistance = Math.max(2, join.getViewDistance());
                onWorld.accept(new RecordedWorld(join.getDimensionTypeRef(), join.getDimensionType(),
                        join.getHashedSeed(), join.isDebug(), join.isFlat(), join.getSeaLevel(),
                        viewDistance));
            });
            return;
        }
        if (type == PacketType.Play.Server.RESPAWN) {
            parseOut(frame, event -> {
                WrapperPlayServerRespawn respawn = new WrapperPlayServerRespawn(event);
                onWorld.accept(new RecordedWorld(respawn.getDimensionTypeRef(), respawn.getDimensionType(),
                        respawn.getHashedSeed(), respawn.isWorldDebug(), respawn.isWorldFlat(),
                        respawn.getSeaLevel(), viewDistance));
            });
            return;
        }

        if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            parseOut(frame, event -> {
                WrapperPlayServerPlayerPositionAndLook teleport = new WrapperPlayServerPlayerPositionAndLook(event);
                Vector3d position = teleport.getPosition();
                follow(position.getX(), position.getZ());
                avatar.rotateTo(teleport.getYaw(), teleport.getPitch());
                avatar.moveTo(position.getX(), position.getY(), position.getZ(), false);
            });
            return;
        }
        if (type == PacketType.Play.Server.PLAYER_ROTATION) {
            parseOut(frame, event -> {
                WrapperPlayServerPlayerRotation rotation = new WrapperPlayServerPlayerRotation(event);
                avatar.rotateTo(rotation.getYaw(), rotation.getPitch());
            });
            return;
        }
        if (type == PacketType.Play.Server.HELD_ITEM_CHANGE) {
            parseOut(frame, event -> avatar.heldSlot(new WrapperPlayServerHeldItemChange(event).getSlot()));
            return;
        }
        if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            parseOut(frame, event -> {
                WrapperPlayServerWindowItems items = new WrapperPlayServerWindowItems(event);
                if (items.getWindowId() == 0) avatar.inventory(items.getItems());
            });
            return;
        }
        if (type == PacketType.Play.Server.SET_SLOT) {
            parseOut(frame, event -> {
                WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);
                if (slot.getWindowId() == 0) avatar.inventorySlot(slot.getSlot(), slot.getItem());
            });
            return;
        }
        if (type == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            parseOut(frame, event -> {
                WrapperPlayServerSetPlayerInventory slot = new WrapperPlayServerSetPlayerInventory(event);
                avatar.inventorySlot(
                        InventoryConstants.playerInventorySlotToContainerSlot(slot.getSlot()),
                        slot.getStack());
            });
            return;
        }

        if (type == PacketType.Play.Server.CHUNK_DATA) {
            world.chunk(frame.packetId(), frame.payload());
            adoptCentre(frame.payload());
            emitChunk(frame.packetId(), frame.payload());
            return;
        }
        if (type == PacketType.Play.Server.UNLOAD_CHUNK) {
            parseOut(frame, event -> {
                WrapperPlayServerUnloadChunk unload = new WrapperPlayServerUnloadChunk(event);
                world.unloadChunk(unload.getChunkX(), unload.getChunkZ());
            });
            sink.send(frame.packetId(), frame.payload());
            return;
        }
        if (type == PacketType.Play.Server.BLOCK_CHANGE) {
            parseOut(frame, event -> {
                WrapperPlayServerBlockChange change = new WrapperPlayServerBlockChange(event);
                Vector3i at = change.getBlockPosition();
                world.blockEdit(at.getX(), at.getY(), at.getZ(), change.getBlockId());
            });
            sink.send(frame.packetId(), frame.payload());
            return;
        }
        if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            parseOut(frame, event -> {
                for (WrapperPlayServerMultiBlockChange.EncodedBlock block
                        : new WrapperPlayServerMultiBlockChange(event).getBlocks()) {
                    world.blockEdit(block.getX(), block.getY(), block.getZ(), block.getBlockId());
                }
            });
            sink.send(frame.packetId(), frame.payload());
            return;
        }
        if (type == PacketType.Play.Server.EXPLOSION) {
            explosion(frame);
            return;
        }

        if (type == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            playerInfoUpdate(frame);
            return;
        }
        if (type == PacketType.Play.Server.PLAYER_INFO) {
            playerInfoLegacy(frame);
            return;
        }
        if (type == PacketType.Play.Server.PLAYER_INFO_REMOVE) {
            parseOut(frame, event ->
                    world.removeProfiles(new WrapperPlayServerPlayerInfoRemove(event).getProfileIds()));
            sink.send(frame.packetId(), frame.payload());
            return;
        }
        if (type == PacketType.Play.Server.TEAMS) {
            parseOut(frame, event -> {
                WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);
                world.team(teams.getTeamName(),
                        teams.getTeamMode() == WrapperPlayServerTeams.TeamMode.REMOVE
                                ? null : new RetainedPacket(frame.packetId(), frame.payload()));
            });
            sink.send(frame.packetId(), frame.payload());
            return;
        }

        if (isBorder(type)) {
            world.border(frame.packetId(), frame.payload());
            sink.send(frame.packetId(), frame.payload());
            return;
        }

        Shape shape = ENTITY_SHAPES.get(type);
        if (shape != null) {
            entity(type, shape, frame);
            return;
        }

        if (type == PacketType.Play.Server.BLOCK_ACTION || type == PacketType.Play.Server.BUNDLE) {
            sink.send(frame.packetId(), frame.payload());
        }
    }

    private boolean isBorder(PacketTypeCommon type) {
        return type == PacketType.Play.Server.INITIALIZE_WORLD_BORDER
                || type == PacketType.Play.Server.WORLD_BORDER
                || type == PacketType.Play.Server.WORLD_BORDER_SIZE
                || type == PacketType.Play.Server.WORLD_BORDER_CENTER
                || type == PacketType.Play.Server.WORLD_BORDER_LERP_SIZE;
    }

    public void recentre() {
        centreX = NO_CENTRE;
        centreZ = NO_CENTRE;
        if (avatar.isPositioned()) follow(avatar.getX(), avatar.getZ());
    }

    private void follow(double x, double z) {
        centre((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    private void adoptCentre(byte[] payload) {
        if (centreX != NO_CENTRE) return;
        long key = ViewerWorld.columnKeyOf(payload);
        if (key == Long.MIN_VALUE) return;
        centre(BlockStore.chunkKeyX(key), BlockStore.chunkKeyZ(key));
    }

    private void centre(int chunkX, int chunkZ) {
        if (chunkX == centreX && chunkZ == centreZ) return;
        centreX = chunkX;
        centreZ = chunkZ;
        sink.send(new WrapperPlayServerUpdateViewPosition(chunkX, chunkZ));
    }

    private void emitChunk(int packetId, byte[] payload) {
        ChunkLight light = this.light;
        if (light == null) {
            sink.send(packetId, payload);
            return;
        }
        int prefix = light.prefixLength(payload);
        if (prefix < 0) {
            if (!light.isSpliced()) emitLegacyLight(light, payload);
            sink.send(packetId, payload);
            return;
        }
        sink.send(packetId, payload, prefix, light.getLitSuffix());
    }

    private void emitLegacyLight(ChunkLight light, byte[] payload) {
        long key = ViewerWorld.columnKeyOf(payload);
        if (key == Long.MIN_VALUE) return;
        sink.send(new WrapperPlayServerUpdateLight(
                BlockStore.chunkKeyX(key), BlockStore.chunkKeyZ(key), light.getFullBright()));
    }

    private void explosion(RecordingFrame.Packet frame) {
        parseOut(frame, event -> {
            WrapperPlayServerExplosion explosion = new WrapperPlayServerExplosion(event);
            Vector3d center = explosion.getPosition();
            for (Vector3i record : explosion.getRecords()) {
                world.blockEdit((int) Math.floor(center.getX()) + record.getX(),
                        (int) Math.floor(center.getY()) + record.getY(),
                        (int) Math.floor(center.getZ()) + record.getZ(), 0);
            }
            explosion.setPlayerMotion(new Vector3f(0f, 0f, 0f));
            explosion.setBuffer(null);
            sink.send(explosion);
        });
    }

    private void playerInfoUpdate(RecordingFrame.Packet frame) {
        parseOut(frame, event -> {
            WrapperPlayServerPlayerInfoUpdate update = new WrapperPlayServerPlayerInfoUpdate(event);
            if (!update.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER)) return;
            List<UUID> added = new ArrayList<>(update.getEntries().size());
            for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : update.getEntries()) {
                UserProfile profile = entry.getGameProfile();
                if (profile == null) continue;
                if (!viewerUuid.equals(profile.getUUID())) added.add(profile.getUUID());
                if (recordedUuid.equals(profile.getUUID())) avatar.adoptTextures(profile);
            }
            world.addProfiles(frame.packetId(), frame.payload(), added);
        });
        sink.send(frame.packetId(), frame.payload());
    }

    private void playerInfoLegacy(RecordingFrame.Packet frame) {
        parseOut(frame, event -> {
            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event);
            if (info.getAction() != WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) return;
            List<UUID> added = new ArrayList<>();
            for (WrapperPlayServerPlayerInfo.PlayerData data : info.getPlayerDataList()) {
                UserProfile profile = data.getUserProfile();
                if (profile == null) continue;
                if (!viewerUuid.equals(profile.getUUID())) added.add(profile.getUUID());
                if (recordedUuid.equals(profile.getUUID())) avatar.adoptTextures(profile);
            }
            world.addProfiles(frame.packetId(), frame.payload(), added);
        });
        sink.send(frame.packetId(), frame.payload());
    }

    private void entity(PacketTypeCommon type, Shape shape, RecordingFrame.Packet frame) {
        byte[] payload = ids.rewrite(shape, frame.payload());
        int packetId = frame.packetId();

        if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            parseOut(frame, event -> {
                for (int id : new WrapperPlayServerDestroyEntities(event).getEntityIds()) {
                    world.destroy(ids.map(id));
                }
            });
            sink.send(packetId, payload);
            return;
        }

        int entityId = shape == Shape.LEADING_INT ? -1 : leadingVarInt(payload);
        if (entityId >= 0) {
            if (type == PacketType.Play.Server.SPAWN_ENTITY
                    || type == PacketType.Play.Server.SPAWN_LIVING_ENTITY
                    || type == PacketType.Play.Server.SPAWN_PLAYER) {
                world.spawn(entityId, packetId, payload);
            } else {
                remember(type, entityId, new RetainedPacket(packetId, payload), frame);
            }
        }
        sink.send(packetId, payload);
    }

    private void remember(PacketTypeCommon type, int entityId, RetainedPacket retained,
                          RecordingFrame.Packet frame) {
        ViewerEntity entity = world.entity(entityId);
        if (entity == null) return;

        if (type == PacketType.Play.Server.ENTITY_METADATA) {
            entity.setMetadata(retained);
        } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            entity.setEquipment(retained);
        } else if (type == PacketType.Play.Server.SET_PASSENGERS) {
            entity.setPassengers(retained);
        } else if (type == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            entity.setAttributes(retained);
        } else if (type == PacketType.Play.Server.ENTITY_EFFECT) {
            parseOut(frame, event -> entity.effect(
                    new WrapperPlayServerEntityEffect(event).getPotionType().getName().toString(),
                    retained));
        } else if (type == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            parseOut(frame, event -> entity.removeEffect(
                    new WrapperPlayServerRemoveEntityEffect(event).getPotionType().getName().toString()));
        } else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            parseOut(frame, event -> {
                Vector3d at = new WrapperPlayServerEntityTeleport(event).getPosition();
                entity.positionedAt(at.getX(), at.getY(), at.getZ());
            });
        } else if (type == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            parseOut(frame, event -> {
                Vector3d at = new WrapperPlayServerEntityPositionSync(event).getValues().getPosition();
                entity.positionedAt(at.getX(), at.getY(), at.getZ());
            });
        }
    }

    public void resync() {
        recentre();
        for (RetainedPacket entry : world.tabList()) sink.send(entry.packetId(), entry.payload());
        for (RetainedPacket team : world.teams()) sink.send(team.packetId(), team.payload());
        for (RetainedPacket border : world.border()) sink.send(border.packetId(), border.payload());

        syncColumns();

        for (Map.Entry<Integer, ViewerEntity> entry : world.entities().entrySet()) {
            ViewerEntity entity = entry.getValue();
            sink.send(entity.getSpawn().packetId(), entity.getSpawn().payload());
            if (entity.isPositioned()) {
                sink.send(new WrapperPlayServerEntityTeleport(entry.getKey(),
                        new Vector3d(entity.getX(), entity.getY(), entity.getZ()), 0f, 0f, false));
            }
            send(entity.getMetadata());
            send(entity.getEquipment());
            send(entity.getAttributes());
            send(entity.getPassengers());
            for (RetainedPacket effect : entity.getEffects().values()) {
                sink.send(effect.packetId(), effect.payload());
            }
        }

        avatar.resend();
    }

    public void wipe() {
        Map<Long, ColumnMirror> snapshot = new HashMap<>();
        for (Map.Entry<Long, ViewerColumn> entry : world.columnMap().entrySet()) {
            ViewerColumn column = entry.getValue();
            snapshot.put(entry.getKey(), new ColumnMirror(column.getChunk().payload(),
                    Map.copyOf(column.getEdits())));
        }
        this.mirror = snapshot;

        int[] entities = world.entities().keySet().stream().mapToInt(Integer::intValue).toArray();
        if (entities.length > 0) sink.send(new WrapperPlayServerDestroyEntities(entities));
        avatar.despawn();
        Set<UUID> profiles = world.profiles();
        if (!profiles.isEmpty() && server.isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
            sink.send(new WrapperPlayServerPlayerInfoRemove(new ArrayList<>(profiles)));
        }
    }

    public void dropMirror() {
        this.mirror = null;
    }

    private void syncColumns() {
        Map<Long, ColumnMirror> before = this.mirror;
        this.mirror = null;

        for (Map.Entry<Long, ViewerColumn> entry : world.columnMap().entrySet()) {
            ViewerColumn column = entry.getValue();
            ColumnMirror had = before == null ? null : before.remove(entry.getKey());
            if (had == null || !Arrays.equals(had.payload(), column.getChunk().payload())
                    || !column.getEdits().keySet().containsAll(had.edits().keySet())) {
                emitChunk(column.getChunk().packetId(), column.getChunk().payload());
                for (Map.Entry<Long, Integer> edit : column.getEdits().entrySet()) {
                    emitBlock(edit.getKey(), edit.getValue());
                }
                continue;
            }
            for (Map.Entry<Long, Integer> edit : column.getEdits().entrySet()) {
                if (Objects.equals(had.edits().get(edit.getKey()), edit.getValue())) continue;
                emitBlock(edit.getKey(), edit.getValue());
            }
        }

        if (before == null) return;
        for (Long key : before.keySet()) {
            sink.send(new WrapperPlayServerUnloadChunk(
                    BlockStore.chunkKeyX(key), BlockStore.chunkKeyZ(key)));
        }
    }

    private void emitBlock(long packed, int state) {
        sink.send(new WrapperPlayServerBlockChange(new Vector3i(
                BlockJournal.unpackX(packed), BlockJournal.unpackY(packed),
                BlockJournal.unpackZ(packed)), state));
    }

    private void send(@Nullable RetainedPacket retained) {
        if (retained != null) sink.send(retained.packetId(), retained.payload());
    }

    private void parseOut(RecordingFrame.Packet frame, Consumer<PacketSendEvent> action) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            scratch.setEncoderState(frame.state());
            ByteBufHelper.writeVarInt(buffer, frame.packetId());
            ByteBufHelper.writeBytes(buffer, frame.payload());
            action.accept(EventCreationUtil.createSendEvent(channel, scratch, null, buffer, true));
        } catch (RuntimeException | LinkageError unreadable) {
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void parseIn(RecordingFrame.Packet frame, Consumer<PacketReceiveEvent> action) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            scratch.setDecoderState(frame.state());
            ByteBufHelper.writeVarInt(buffer, frame.packetId());
            ByteBufHelper.writeBytes(buffer, frame.payload());
            action.accept(EventCreationUtil.createReceiveEvent(channel, scratch, null, buffer, true));
        } catch (RuntimeException | LinkageError unreadable) {
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private record ColumnMirror(byte[] payload, Map<Long, Integer> edits) {
    }
}
