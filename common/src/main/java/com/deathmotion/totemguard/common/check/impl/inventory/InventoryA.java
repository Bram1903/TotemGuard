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

package com.deathmotion.totemguard.common.check.impl.inventory;

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.List;
import java.util.function.Predicate;

@CheckData(description = "Impossible action with open inventory", type = CheckType.INVENTORY)
public class InventoryA extends CheckImpl implements PacketCheck {

    private static final List<Rule> RULES = List.of(
            Rule.of(
                    "attack",
                    PacketType.Play.Client.INTERACT_ENTITY,
                    event -> new WrapperPlayClientInteractEntity(event).getAction()
                            == WrapperPlayClientInteractEntity.InteractAction.ATTACK
            ),
            Rule.simple("place", PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT),
            Rule.of(
                    "break",
                    PacketType.Play.Client.PLAYER_DIGGING,
                    event -> new WrapperPlayClientPlayerDigging(event).getAction()
                            == DiggingAction.START_DIGGING
            ),
            Rule.simple("move", PacketType.Play.Client.PLAYER_INPUT),
            Rule.simple("change slot", PacketType.Play.Client.HELD_ITEM_CHANGE)
    );

    public InventoryA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getData().isSprinting() && event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            fail("sprinting");
        }

        if (!player.getData().isOpenInventory()) return;

        for (Rule rule : RULES) {
            if (rule.matches(event)) {
                fail(rule.name());

                // Yes, I now this is stupid,
                // but clients like Wurst and Meteor just don't close the inventory after interacting with the inventory
                player.getData().setOpenInventory(false);
                return;
            }
        }
    }

    private record Rule(String name, PacketTypeCommon type, Predicate<PacketReceiveEvent> predicate) {

        static Rule simple(String name, PacketTypeCommon type) {
            return new Rule(name, type, event -> true);
        }

        static Rule of(String name, PacketTypeCommon type, Predicate<PacketReceiveEvent> predicate) {
            return new Rule(name, type, predicate);
        }

        boolean matches(PacketReceiveEvent event) {
            return event.getPacketType() == type && predicate.test(event);
        }
    }
}


