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
import com.deathmotion.totemguard.common.player.data.EffectData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;

public class OutboundEffectProcessor extends ProcessorOutbound {

    private final EffectData effectData;
    private final PacketLatencyHandler latencyHandler;

    public OutboundEffectProcessor(TGPlayer player) {
        super(player);
        this.effectData = player.getData().getEffectData();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.ENTITY_EFFECT) {
            WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(event);
            if (packet.getEntityId() != player.getUser().getEntityId()) return;

            PotionType potion = packet.getPotionType();
            int amplifier = packet.getEffectAmplifier();
            int duration = packet.getEffectDurationTicks();
            if (potion == PotionTypes.LEVITATION) {
                latencyHandler.compensate(event, () -> effectData.setLevitation(amplifier, duration));
            } else if (potion == PotionTypes.JUMP_BOOST) {
                latencyHandler.compensate(event, () -> effectData.setJumpBoost(amplifier, duration));
            } else if (potion == PotionTypes.SLOW_FALLING) {
                latencyHandler.compensate(event, () -> effectData.setSlowFalling(duration));
            } else if (potion == PotionTypes.DOLPHINS_GRACE) {
                latencyHandler.compensate(event, () -> effectData.setDolphinsGrace(duration));
            } else if (potion == PotionTypes.WEAVING && observesWeaving()) {
                latencyHandler.compensate(event, () -> effectData.setWeaving(duration));
            } else if (potion == PotionTypes.HASTE) {
                latencyHandler.compensate(event, () -> effectData.setHaste(amplifier, duration));
            } else if (potion == PotionTypes.CONDUIT_POWER) {
                latencyHandler.compensate(event, () -> effectData.setConduitPower(amplifier, duration));
            } else if (potion == PotionTypes.MINING_FATIGUE) {
                latencyHandler.compensate(event, () -> effectData.setMiningFatigue(amplifier, duration));
            }
        } else if (type == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrapperPlayServerRemoveEntityEffect packet = new WrapperPlayServerRemoveEntityEffect(event);
            if (packet.getEntityId() != player.getUser().getEntityId()) return;

            PotionType potion = packet.getPotionType();
            if (potion == PotionTypes.LEVITATION) {
                latencyHandler.compensate(event, effectData::clearLevitation);
            } else if (potion == PotionTypes.JUMP_BOOST) {
                latencyHandler.compensate(event, effectData::clearJumpBoost);
            } else if (potion == PotionTypes.SLOW_FALLING) {
                latencyHandler.compensate(event, effectData::clearSlowFalling);
            } else if (potion == PotionTypes.DOLPHINS_GRACE) {
                latencyHandler.compensate(event, effectData::clearDolphinsGrace);
            } else if (potion == PotionTypes.WEAVING) {
                latencyHandler.compensate(event, effectData::clearWeaving);
            } else if (potion == PotionTypes.HASTE) {
                latencyHandler.compensate(event, effectData::clearHaste);
            } else if (potion == PotionTypes.CONDUIT_POWER) {
                latencyHandler.compensate(event, effectData::clearConduitPower);
            } else if (potion == PotionTypes.MINING_FATIGUE) {
                latencyHandler.compensate(event, effectData::clearMiningFatigue);
            }
        }
    }

    private boolean observesWeaving() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5);
    }
}
