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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.config.schema.EntitySpoofingOptions;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.util.MetadataIndex;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import java.util.concurrent.ThreadLocalRandom;

public class OutboundMetadataProcessor extends ProcessorOutbound {

    private static final float MIN_SPOOFED_HEALTH = 0.5F;
    private static final float MAX_SPOOFED_HEALTH = 20.0F;

    private static final String[] COMPETING_PLUGINS = {
            "AntiHealthIndicator", "antihealthindicator", "PolarLoader"
    };
    private static final boolean COMPETING_PLUGIN_ACTIVE = detectCompetingPlugin();

    private final Data data;
    private final EntityTracker entities;
    private final PacketLatencyHandler latencyHandler;
    private final ConfigRepositoryImpl configRepository;
    private final int healthIndex;
    private final int absorptionIndex;
    private final int slimeSizeIndex;
    private final int livingFlagsIndex;

    public OutboundMetadataProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.entities = player.getWorldMirror().entities();
        this.latencyHandler = player.getLatencyHandler();
        this.configRepository = TGPlatform.getInstance().getConfigRepository();
        MetadataIndex metadataIndex = player.getMetadataIndex();
        this.healthIndex = metadataIndex.health();
        this.absorptionIndex = metadataIndex.absorption();
        this.slimeSizeIndex = metadataIndex.slimeSize();
        this.livingFlagsIndex = metadataIndex.livingEntityFlags();
    }

    private static boolean detectCompetingPlugin() {
        TGPlatform platform = TGPlatform.getInstance();
        for (String name : COMPETING_PLUGINS) {
            if (platform.isPluginEnabled(name)) {
                platform.getLogger().info(
                        "Detected " + name + ". Disabling TotemGuard entity-spoofing (health/absorption) to avoid unnecessary computing."
                );
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        int entityId = packet.getEntityId();

        if (entityId == player.getUser().getEntityId()) {
            trackOwnMetadata(event, packet);
            return;
        }

        trackSlimeSize(event, entityId, packet);

        if (COMPETING_PLUGIN_ACTIVE) return;

        EntitySpoofingOptions options = configRepository.configView().entitySpoofing();
        if (!options.health() && !options.absorption()) return;
        if (!entities.isPlayer(entityId)) return;

        boolean modified = false;
        for (EntityData<?> meta : packet.getEntityMetadata()) {
            int index = meta.getIndex();
            if (options.health() && index == healthIndex && spoofHealth(meta)) {
                modified = true;
            } else if (options.absorption() && index == absorptionIndex && spoofAbsorption(meta)) {
                modified = true;
            }
        }

        if (modified) event.markForReEncode(true);
    }

    private void trackSlimeSize(PacketSendEvent event, int entityId, WrapperPlayServerEntityMetadata packet) {
        if (slimeSizeIndex < 0) return;
        if (!entities.isSlimeLike(entityId)) return;
        for (EntityData<?> meta : packet.getEntityMetadata()) {
            if (meta.getIndex() != slimeSizeIndex) continue;
            if (meta.getValue() instanceof Integer size) {
                latencyHandler.compensateLazy(event, () -> entities.setSlimeSize(entityId, size));
            }
            return;
        }
    }

    // Server-forced flips (glide cancel, pose) only apply when the packet arrives, so applying
    // them a round trip early was a false-excess source.
    private void trackOwnMetadata(PacketSendEvent event, WrapperPlayServerEntityMetadata packet) {
        for (EntityData<?> meta : packet.getEntityMetadata()) {
            int index = meta.getIndex();
            Object value = meta.getValue();
            if (index == 0 && value instanceof Byte sharedFlags) {
                final boolean swimming = (sharedFlags & 0x10) != 0;
                final boolean gliding = (sharedFlags & 0x80) != 0;
                latencyHandler.compensateLazy(event, () -> {
                    data.setSwimming(swimming);
                    data.setGliding(gliding);
                });
            } else if (index == livingFlagsIndex && value instanceof Byte livingFlags) {
                final boolean spinAttacking = (livingFlags & 0x04) != 0;
                latencyHandler.compensateLazy(event, () -> data.setSpinAttacking(spinAttacking));
            } else if (value instanceof EntityPose pose) {
                final boolean sleeping = pose == EntityPose.SLEEPING;
                latencyHandler.compensateLazy(event, () -> data.setSleeping(sleeping));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean spoofHealth(EntityData<?> meta) {
        Object value = meta.getValue();
        // Preserve <=0 so the death animation/red overlay still triggers client-side.
        if (!(value instanceof Float floatValue) || floatValue <= 0.0F) return false;
        float spoofed = MIN_SPOOFED_HEALTH + ThreadLocalRandom.current().nextFloat() * (MAX_SPOOFED_HEALTH - MIN_SPOOFED_HEALTH);
        ((EntityData<Float>) meta).setValue(spoofed);
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean spoofAbsorption(EntityData<?> meta) {
        Object value = meta.getValue();
        if (!(value instanceof Float floatValue) || floatValue <= 0.0F) return false;
        ((EntityData<Float>) meta).setValue(0.0F);
        return true;
    }
}
