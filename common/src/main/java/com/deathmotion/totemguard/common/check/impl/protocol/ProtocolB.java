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

package com.deathmotion.totemguard.common.check.impl.protocol;

import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.HashSet;
import java.util.Set;

/*
 * This check can be handled post, because we are not canceling the hits, but only monitoring them.
 * By handling it post, we don't have to flag for multiple violations per tick
 */
@CheckData(description = "Attacked multiple entities in the same tick")
public class ProtocolB extends CheckImpl implements PacketCheck {

    private final Set<Integer> entities = new HashSet<>();

    public ProtocolB(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.INTERACT_ENTITY) {
            final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }

            entities.add(packet.getEntityId());
            return;
        }

        if (player.isTickEndPacket(packetType)) {
            final int uniqueTargets = entities.size();

            if (uniqueTargets > 1) {
                fail("entities=" + uniqueTargets);
            }

            entities.clear();
        }
    }
}

