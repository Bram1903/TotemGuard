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

package com.deathmotion.totemguard.common.replay.capture;

import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class CaptureFilter {

    private static final Set<PacketTypeCommon> CONNECTION = Set.of(
            PacketType.Handshaking.Client.HANDSHAKE,
            PacketType.Login.Client.LOGIN_START,
            PacketType.Login.Server.LOGIN_SUCCESS,
            PacketType.Login.Client.LOGIN_SUCCESS_ACK,
            PacketType.Configuration.Server.CONFIGURATION_END,
            PacketType.Configuration.Client.CONFIGURATION_END_ACK,
            PacketType.Configuration.Client.PLUGIN_MESSAGE,
            PacketType.Play.Server.CONFIGURATION_START,
            PacketType.Play.Client.CONFIGURATION_ACK
    );

    private static final Set<PacketTypeCommon> MOVEMENT = Set.of(
            PacketType.Play.Client.CLIENT_TICK_END,
            PacketType.Play.Client.TELEPORT_CONFIRM,
            PacketType.Play.Client.PLAYER_INPUT,
            PacketType.Play.Client.STEER_VEHICLE,
            PacketType.Play.Client.VEHICLE_MOVE,
            PacketType.Play.Client.PLAYER_ABILITIES,
            PacketType.Play.Client.SPECTATE
    );

    private static final Set<PacketTypeCommon> ACTIONS = Set.of(
            PacketType.Play.Client.ANIMATION,
            PacketType.Play.Client.ATTACK,
            PacketType.Play.Client.INTERACT_ENTITY,
            PacketType.Play.Client.ENTITY_ACTION,
            PacketType.Play.Client.PLAYER_DIGGING,
            PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT,
            PacketType.Play.Client.USE_ITEM,
            PacketType.Play.Client.HELD_ITEM_CHANGE,
            PacketType.Play.Client.PICK_ITEM,
            PacketType.Play.Client.PICK_ITEM_FROM_BLOCK,
            PacketType.Play.Client.PICK_ITEM_FROM_ENTITY,
            PacketType.Play.Client.NAME_ITEM,
            PacketType.Play.Client.CLICK_WINDOW,
            PacketType.Play.Client.CLOSE_WINDOW,
            PacketType.Play.Client.CREATIVE_INVENTORY_ACTION,
            PacketType.Play.Client.PLUGIN_MESSAGE,
            PacketType.Play.Client.KEEP_ALIVE,
            PacketType.Play.Client.PONG
    );

    private static final Set<PacketTypeCommon> WORLD = Set.of(
            PacketType.Play.Server.JOIN_GAME,
            PacketType.Play.Server.RESPAWN,
            PacketType.Play.Server.CHUNK_DATA,
            PacketType.Play.Server.MAP_CHUNK_BULK,
            PacketType.Play.Server.UNLOAD_CHUNK,
            PacketType.Play.Server.BLOCK_CHANGE,
            PacketType.Play.Server.MULTI_BLOCK_CHANGE,
            PacketType.Play.Server.ACKNOWLEDGE_BLOCK_CHANGES,
            PacketType.Play.Server.BLOCK_ACTION,
            PacketType.Play.Server.EXPLOSION,
            PacketType.Play.Server.INITIALIZE_WORLD_BORDER,
            PacketType.Play.Server.WORLD_BORDER,
            PacketType.Play.Server.WORLD_BORDER_SIZE,
            PacketType.Play.Server.WORLD_BORDER_CENTER,
            PacketType.Play.Server.WORLD_BORDER_LERP_SIZE
    );

    private static final Set<PacketTypeCommon> ENTITIES = Set.of(
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.SPAWN_LIVING_ENTITY,
            PacketType.Play.Server.SPAWN_PLAYER,
            PacketType.Play.Server.DESTROY_ENTITIES,
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE,
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_POSITION_SYNC,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ENTITY_STATUS,
            PacketType.Play.Server.ENTITY_EFFECT,
            PacketType.Play.Server.REMOVE_ENTITY_EFFECT,
            PacketType.Play.Server.UPDATE_ATTRIBUTES,
            PacketType.Play.Server.SET_PASSENGERS,
            PacketType.Play.Server.TEAMS,
            PacketType.Play.Server.PLAYER_INFO,
            PacketType.Play.Server.PLAYER_INFO_UPDATE,
            PacketType.Play.Server.PLAYER_INFO_REMOVE
    );

    private static final Set<PacketTypeCommon> PLAYER = Set.of(
            PacketType.Play.Server.PLAYER_POSITION_AND_LOOK,
            PacketType.Play.Server.PLAYER_ROTATION,
            PacketType.Play.Server.PLAYER_ABILITIES,
            PacketType.Play.Server.UPDATE_HEALTH,
            PacketType.Play.Server.SET_EXPERIENCE,
            PacketType.Play.Server.CHANGE_GAME_STATE,
            PacketType.Play.Server.CAMERA,
            PacketType.Play.Server.HELD_ITEM_CHANGE,
            PacketType.Play.Server.WINDOW_ITEMS,
            PacketType.Play.Server.SET_SLOT,
            PacketType.Play.Server.SET_CURSOR_ITEM,
            PacketType.Play.Server.SET_PLAYER_INVENTORY,
            PacketType.Play.Server.OPEN_WINDOW,
            PacketType.Play.Server.OPEN_HORSE_WINDOW,
            PacketType.Play.Server.CLOSE_WINDOW,
            PacketType.Play.Server.PING,
            PacketType.Play.Server.KEEP_ALIVE,
            PacketType.Play.Server.BUNDLE
    );

    private CaptureFilter() {
    }

    public static boolean keptVelocity(int entityId, int playerId, int vehicleId, @Nullable EntityType type) {
        if (entityId == playerId || entityId == vehicleId) return true;
        if (type == EntityTypes.PLAYER) return true;
        return EntityRoles.clientSimulatesFromVelocity(type);
    }

    public static boolean kept(PacketTypeCommon type) {
        return WrapperPlayClientPlayerFlying.isFlying(type)
                || MOVEMENT.contains(type)
                || ENTITIES.contains(type)
                || WORLD.contains(type)
                || ACTIONS.contains(type)
                || PLAYER.contains(type)
                || CONNECTION.contains(type);
    }
}
