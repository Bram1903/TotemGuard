/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.checks.impl.badpackets;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;

import java.util.concurrent.TimeUnit;

@CheckData(name = "BadPacketsD", description = "Tries to impersonate Lunar Client", experimental = true)
public class BadPacketsD extends Check implements PacketCheck {

    private boolean hasBeenChecked = false;
    private Location lastLocation;

    public BadPacketsD(final TotemPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (hasBeenChecked) return;

        final Location loc = extractLocation(event);
        if (loc == null) return;

        if (lastLocation == null) {
            lastLocation = loc;
            return;
        }

        if (positionChanged(lastLocation, loc)) {
            handle();
        }
    }

    private Location extractLocation(final PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.PLAYER_POSITION) {
            return new WrapperPlayClientPlayerPosition(event).getLocation();
        }
        if (type == PacketType.Play.Client.PLAYER_ROTATION) {
            return new WrapperPlayClientPlayerRotation(event).getLocation();
        }
        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return new WrapperPlayClientPlayerPositionAndRotation(event).getLocation();
        }
        return null;
    }

    private boolean positionChanged(final Location a, final Location b) {
        return a.getX() != b.getX() || a.getY() != b.getY() || a.getZ() != b.getZ();
    }

    private void handle() {
        hasBeenChecked = true;

        final long ping = Math.max(0L, player.getPing());
        final long calculatedDelayMs = (ping + 5L) * 5L;

        FoliaScheduler.getAsyncScheduler().runDelayed(TotemGuard.getInstance(), o -> {
            final String clientBrand = player.getBrand();
            final boolean claimsLunar = clientBrand.toLowerCase().contains("lunarclient");

            if (claimsLunar && !player.isUsingLunarClient) {
                fail(createDetails(clientBrand));
            }
        }, calculatedDelayMs, TimeUnit.MILLISECONDS);
    }

    private Component createDetails(final String clientBrand) {
        return Component.text()
                .append(Component.text("Fake Lunar Client Brand: ", color.getX()))
                .append(Component.text(clientBrand, color.getY()))
                .build();
    }
}
