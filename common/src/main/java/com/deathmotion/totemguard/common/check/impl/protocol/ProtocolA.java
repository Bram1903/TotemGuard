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

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.TickData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

@CheckData(description = "Impossible action combination", type = CheckType.PROTOCOL)
public class ProtocolA extends CheckImpl implements PacketCheck {

    private static final List<Rule> RULES = List.of(
            new Rule("attack + place", t -> t.isAttacking() && t.isPlacing()),
            new Rule("attack + releasing", t -> t.isAttacking() && t.isReleasing()),
            new Rule("inventory_click + place", t -> t.isClickingInInventory() && t.isPlacing()),
            new Rule("inventory_click + attack", t -> t.isClickingInInventory() && t.isAttacking()),
            new Rule("quickmove + attack", t -> t.isQuickMoveClicking() && t.isAttacking()),
            new Rule("pickup_click + place", t -> t.isPickUpClicking() && t.isPlacing())
    );

    public ProtocolA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.isTickEndPacket(event.getPacketType())) return;

        TickData t = player.getTickData();

        for (Rule rule : RULES) {
            if (rule.predicate().test(t)) {
                if (rule.name.equals("attack + releasing")) {
                    ItemStack mainHandItem = player.getInventory().getMainHandItem();
                    ItemStack offHandItem = player.getInventory().getOffhandItem();

                    fail("main=" + mainHandItem.getType().getName().getKey() + ",off=" + offHandItem.getType().getName().getKey());
                    return;
                }

                fail(rule.name);
            }
        }
    }

    private record Rule(String name, Predicate<TickData> predicate) {
    }
}
