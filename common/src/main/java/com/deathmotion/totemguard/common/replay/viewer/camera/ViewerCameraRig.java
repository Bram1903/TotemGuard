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

package com.deathmotion.totemguard.common.replay.viewer.camera;

import com.deathmotion.totemguard.common.replay.viewer.net.ViewerSink;
import com.deathmotion.totemguard.common.replay.viewer.state.ViewerAvatar;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ViewerCameraRig {

    public static final int ENTITY_ID = ViewerAvatar.ENTITY_ID - 2;

    private static final double EYE_HEIGHT = 1.7775;
    private static final byte INVISIBLE = 0x20;
    private static final UUID ID = UUID.nameUUIDFromBytes(
            "TotemGuardReplayCamera".getBytes(StandardCharsets.UTF_8));

    private final ViewerSink sink;
    private final ServerVersion server;

    private boolean spawned;

    public ViewerCameraRig(ViewerSink sink, ServerVersion server) {
        this.sink = sink;
        this.server = server;
    }

    private static List<EntityData<?>> hidden() {
        return List.of(new EntityData<>(0, EntityDataTypes.BYTE, INVISIBLE));
    }

    private static Vector3d anchor(CameraPose pose) {
        return new Vector3d(pose.x(), pose.y() - EYE_HEIGHT, pose.z());
    }

    public void show(CameraPose pose) {
        if (spawned) {
            move(pose);
            return;
        }
        Vector3d anchor = anchor(pose);
        if (server.isNewerThanOrEquals(ServerVersion.V_1_20_2)) {
            sink.send(new WrapperPlayServerSpawnEntity(ENTITY_ID, Optional.of(ID),
                    EntityTypes.ARMOR_STAND, anchor, pose.pitch(), pose.yaw(), pose.yaw(),
                    0, Optional.empty()));
        } else {
            sink.send(new WrapperPlayServerSpawnLivingEntity(ENTITY_ID, ID,
                    EntityTypes.ARMOR_STAND, anchor, pose.yaw(), pose.pitch(), pose.yaw(),
                    new Vector3d(), hidden()));
        }
        spawned = true;
        sink.send(new WrapperPlayServerEntityMetadata(ENTITY_ID, hidden()));
        move(pose);
    }

    public void move(CameraPose pose) {
        if (!spawned) return;
        sink.send(new WrapperPlayServerEntityTeleport(ENTITY_ID, anchor(pose),
                pose.yaw(), pose.pitch(), false));
        sink.send(new WrapperPlayServerEntityHeadLook(ENTITY_ID, pose.yaw()));
    }

    public void hide() {
        if (!spawned) return;
        spawned = false;
        sink.send(new WrapperPlayServerDestroyEntities(ENTITY_ID));
    }

    public void forget() {
        spawned = false;
    }

    public boolean isSpawned() {
        return spawned;
    }
}
