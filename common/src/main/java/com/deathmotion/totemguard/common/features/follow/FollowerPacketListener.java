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

package com.deathmotion.totemguard.common.features.follow;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class FollowerPacketListener extends PacketListenerAbstract {

    private final FollowRepository followRepository;

    public FollowerPacketListener(@NotNull FollowRepository followRepository) {
        super(PacketListenerPriority.MONITOR);
        this.followRepository = followRepository;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        UUID uuid = event.getUser().getUUID();
        if (uuid == null) return;
        if (!followRepository.isFollowing(uuid)) return;

        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
        if (!packet.hasPositionChanged()) return;
        Location loc = packet.getLocation();
        followRepository.updateFollowerPosition(uuid, loc.getX(), loc.getY(), loc.getZ());
    }
}
