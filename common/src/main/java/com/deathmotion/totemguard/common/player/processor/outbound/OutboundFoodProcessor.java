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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.FoodData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;

public class OutboundFoodProcessor extends ProcessorOutbound {

    private static final int SPRINT_FOOD_THRESHOLD = 6;

    private final FoodData foodData;
    private final PacketLatencyHandler latencyHandler;

    private boolean lastSentCanSprint = true;

    public OutboundFoodProcessor(TGPlayer player) {
        super(player);
        this.foodData = player.getData().getFoodData();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.UPDATE_HEALTH) return;

        WrapperPlayServerUpdateHealth health = new WrapperPlayServerUpdateHealth(event);
        player.getData().setDead(health.getHealth() <= 0.0f);

        boolean canSprint = health.getFood() > SPRINT_FOOD_THRESHOLD;
        if (canSprint == lastSentCanSprint) return;
        lastSentCanSprint = canSprint;

        if (canSprint) {
            foodData.setCanSprint(true);
        } else {
            latencyHandler.compensate(event, () -> {
                if (!lastSentCanSprint) foodData.setCanSprint(false);
            });
        }
    }
}
