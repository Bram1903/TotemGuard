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

import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.format.RecordingHeader;
import com.deathmotion.totemguard.common.replay.format.RecordingReader;
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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CameraScan {

    private final ClientVersion wire;
    private final User scratch;
    private final Object channel = new Object();
    private final List<CameraTrack.Sample> samples = new ArrayList<>();

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean seeded;

    private CameraScan(ServerVersion server, RecordingHeader header) {
        this.wire = server.toClientVersion();
        this.scratch = new User(null, ConnectionState.PLAY, header.client(),
                new UserProfile(header.playerUuid(), header.playerName()));
        this.scratch.setClientVersion(header.client());
    }

    public static @Nullable CameraTrack scan(Path file, ServerVersion server, RecordingHeader header) {
        CameraScan scan = new CameraScan(server, header);
        try (RecordingReader reader = new RecordingReader(file)) {
            RecordingFrame frame;
            while ((frame = reader.next()) != null) {
                if (scan.samples.size() >= CameraTrack.MAX_SAMPLES) break;
                if (frame instanceof RecordingFrame.Packet packet
                        && packet.state() == ConnectionState.PLAY) {
                    scan.read(packet);
                }
            }
        } catch (Exception unreadable) {
            return CameraTrack.of(scan.samples);
        }
        return CameraTrack.of(scan.samples);
    }

    private void read(RecordingFrame.Packet frame) {
        if (frame.inbound()) {
            PacketTypeCommon type = PacketType.Play.Client.getById(wire, frame.packetId());
            if (type == null || !WrapperPlayClientPlayerFlying.isFlying(type)) return;
            parseIn(frame, event -> {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                if (flying.hasPositionChanged()) {
                    Vector3d position = flying.getLocation().getPosition();
                    x = position.getX();
                    y = position.getY();
                    z = position.getZ();
                }
                if (flying.hasRotationChanged()) {
                    yaw = flying.getLocation().getYaw();
                    pitch = flying.getLocation().getPitch();
                }
                if (flying.hasPositionChanged() || flying.hasRotationChanged()) {
                    take(frame.nanos());
                }
            });
            return;
        }

        PacketTypeCommon type = PacketType.Play.Server.getById(wire, frame.packetId());
        if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            parseOut(frame, event -> {
                WrapperPlayServerPlayerPositionAndLook teleport =
                        new WrapperPlayServerPlayerPositionAndLook(event);
                Vector3d position = teleport.getPosition();
                x = position.getX();
                y = position.getY();
                z = position.getZ();
                yaw = teleport.getYaw();
                pitch = teleport.getPitch();
                take(frame.nanos());
            });
            return;
        }
        if (type == PacketType.Play.Server.PLAYER_ROTATION) {
            parseOut(frame, event -> {
                WrapperPlayServerPlayerRotation rotation = new WrapperPlayServerPlayerRotation(event);
                yaw = rotation.getYaw();
                pitch = rotation.getPitch();
                take(frame.nanos());
            });
        }
    }

    private void take(long nanos) {
        if (!seeded) {
            seeded = true;
        } else if (nanos <= samples.get(samples.size() - 1).nanos()) {
            return;
        }
        samples.add(new CameraTrack.Sample(nanos, x, y, z, yaw, pitch));
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
}
