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

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.HashedStack;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

import java.util.Optional;

@CheckData(name = "BadPacketsD", description = "Invalid totem pickup packet")
public class BadPacketsD extends Check implements PacketCheck {

    private final boolean legacyItemStack;

    public BadPacketsD(final TotemPlayer player) {
        super(player);
        this.legacyItemStack = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_21_4);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

//        final WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
//        if (packet.getWindowClickType() != WrapperPlayClientClickWindow.WindowClickType.PICKUP) return;
//
//        if (legacyItemStack) {
//            ItemStack item = packet.getCarriedItemStack();
//            if (item.getType() == ItemTypes.TOTEM_OF_UNDYING) fail();
//        } else {
//            Optional<HashedStack> item = packet.getCarriedHashedStack();
//            if (item.isPresent() && item.get().getItem() == ItemTypes.TOTEM_OF_UNDYING) fail();
//        }
    }
}
