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

package com.deathmotion.totemguard.common.check.impl.protocol;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.Nullable;

@RequiresTickEnd
@CheckData(description = "Slot change after action in same tick", type = CheckType.PROTOCOL)
public class ProtocolA extends CheckImpl implements PacketCheck {

    private @Nullable String flushedBy;

    public ProtocolA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.CLIENT_TICK_END) {
            flushedBy = null;
            return;
        }

        if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (flushedBy == null || ticksMayBeMerged()) return;
            fail("action={0},slot={1}", flushedBy, new WrapperPlayClientHeldItemChange(event).getSlot());
            return;
        }

        if (flushedBy == null) {
            flushedBy = flushingAction(type, event);
        }
    }

    // startDestroyBlock never flushes the slot, a drop only flushes on a 26.3 client, and Polar writes releases of its own
    private @Nullable String flushingAction(PacketTypeCommon type, PacketReceiveEvent event) {
        if (type == PacketType.Play.Client.ATTACK) return "attack";
        if (type == PacketType.Play.Client.USE_ITEM) return "use";
        if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return "place";
        if (type == PacketType.Play.Client.PICK_ITEM_FROM_BLOCK || type == PacketType.Play.Client.PICK_ITEM_FROM_ENTITY) {
            return "pick";
        }
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            return switch (new WrapperPlayClientInteractEntity(event).getAction()) {
                case ATTACK -> "attack";
                case INTERACT -> "interact";
                case INTERACT_AT -> "interact at";
            };
        }
        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            return switch (new WrapperPlayClientPlayerDigging(event).getAction()) {
                case CHANGE_DESTROY_DIRECTION -> "face";
                case FINISHED_DIGGING -> "finish";
                case RELEASE_USE_ITEM -> platform.isPolarLoaded() ? null : "release";
                case STAB -> "stab";
                case DROP_ITEM, DROP_ITEM_STACK -> player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)
                        ? "drop"
                        : null;
                default -> null;
            };
        }
        return null;
    }
}
