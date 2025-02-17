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
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import net.kyori.adventure.text.Component;

@CheckData(name = "BadPacketsC", description = "Impossible same slot packet")
public class BadPacketsC extends Check implements PacketCheck {
    int lastSlot = -69;

    public BadPacketsC(final TotemPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.HELD_ITEM_CHANGE) return;

        final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
        if (slot == lastSlot) {
            fail(createDetails(slot, lastSlot));
        }

        lastSlot = slot;
    }

    private Component createDetails(int slot, int lastSlot) {
        return Component.text()
                .append(Component.text("New Slot Change: ", color.getX()))
                .append(Component.text(slot, color.getY()))
                .append(Component.newline())
                .append(Component.text("Last Slot Change: ", color.getX()))
                .append(Component.text(lastSlot, color.getY()))
                .build();
    }
}
