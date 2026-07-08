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
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;

public class OutboundPistonProcessor extends ProcessorOutbound {

    private static final double ARM_RANGE = 15.0;
    private static final double ARM_RANGE_SQUARED = ARM_RANGE * ARM_RANGE;
    private static final int MAX_PUSH_DEPTH = 12;

    private final Data data;
    private final WorldMirror world;
    private final PacketLatencyHandler latencyHandler;

    public OutboundPistonProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.world = player.getWorldMirror();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.BLOCK_ACTION) return;

        WrapperPlayServerBlockAction packet = new WrapperPlayServerBlockAction(event);
        StateType type = packet.getBlockType().getType();
        if (type != StateTypes.PISTON && type != StateTypes.STICKY_PISTON) return;

        Vector3i pos = packet.getBlockPosition();
        int actionId = packet.getActionId();
        int direction = packet.getActionData();
        latencyHandler.compensate(event, () -> resolve(pos, actionId, direction));
    }

    private void resolve(Vector3i pos, int actionId, int direction) {
        if (actionId != 0 && actionId != 1) return;
        Location current = data.getMovementData().getCurrent();
        double dx = (pos.getX() + 0.5) - current.getX();
        double dy = (pos.getY() + 0.5) - current.getY();
        double dz = (pos.getZ() + 0.5) - current.getZ();
        if (dx * dx + dy * dy + dz * dz > ARM_RANGE_SQUARED) return;

        int stepX = DIR_X[direction & 7];
        int stepY = DIR_Y[direction & 7];
        int stepZ = DIR_Z[direction & 7];
        int pushX = actionId == 0 ? stepX : -stepX;
        int pushY = actionId == 0 ? stepY : -stepY;
        int pushZ = actionId == 0 ? stepZ : -stepZ;

        BlockReader reader = world.reader();
        int baseX = pos.getX() + stepX;
        int baseY = pos.getY() + stepY;
        int baseZ = pos.getZ() + stepZ;

        int minX = Math.min(pos.getX(), baseX), minY = Math.min(pos.getY(), baseY), minZ = Math.min(pos.getZ(), baseZ);
        int maxX = Math.max(pos.getX(), baseX), maxY = Math.max(pos.getY(), baseY), maxZ = Math.max(pos.getZ(), baseZ);
        boolean slimeFront = false;
        boolean honeyFront = false;

        if (actionId == 0) {
            int cx = baseX, cy = baseY, cz = baseZ;
            for (int i = 0; i < MAX_PUSH_DEPTH; i++) {
                StateType front = reader.state(cx, cy, cz).getType();
                if (!isPushable(reader, cx, cy, cz)) break;
                if (front == StateTypes.SLIME_BLOCK) slimeFront = true;
                if (front == StateTypes.HONEY_BLOCK) honeyFront = true;
                minX = Math.min(minX, cx + pushX);
                minY = Math.min(minY, cy + pushY);
                minZ = Math.min(minZ, cz + pushZ);
                maxX = Math.max(maxX, cx + pushX);
                maxY = Math.max(maxY, cy + pushY);
                maxZ = Math.max(maxZ, cz + pushZ);
                cx += stepX;
                cy += stepY;
                cz += stepZ;
            }
        } else {
            StateType attached = reader.state(baseX, baseY, baseZ).getType();
            if (attached == StateTypes.SLIME_BLOCK) slimeFront = true;
            if (attached == StateTypes.HONEY_BLOCK) honeyFront = true;
            minX = Math.min(minX, baseX + pushX);
            minY = Math.min(minY, baseY + pushY);
            minZ = Math.min(minZ, baseZ + pushZ);
            maxX = Math.max(maxX, baseX + pushX);
            maxY = Math.max(maxY, baseY + pushY);
            maxZ = Math.max(maxZ, baseZ + pushZ);
        }

        data.getPistonData().arm(pushX, pushY, pushZ, slimeFront, honeyFront,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean isPushable(BlockReader reader, int x, int y, int z) {
        long facts = reader.facts(x, y, z);
        if (StateFacts.is(facts, StateFacts.AIR) || StateFacts.is(facts, StateFacts.ANY_FLUID)) return false;
        if (!StateFacts.is(facts, StateFacts.HAS_SHAPE)) return false;
        StateType type = reader.state(x, y, z).getType();
        return type != StateTypes.OBSIDIAN && type != StateTypes.CRYING_OBSIDIAN
                && type != StateTypes.RESPAWN_ANCHOR && type != StateTypes.BEDROCK
                && type != StateTypes.PISTON && type != StateTypes.STICKY_PISTON
                && type != StateTypes.MOVING_PISTON && type != StateTypes.PISTON_HEAD;
    }

    private static final int[] DIR_X = {0, 0, 0, 0, -1, 1, 0, 0};
    private static final int[] DIR_Y = {-1, 1, 0, 0, 0, 0, 0, 0};
    private static final int[] DIR_Z = {0, 0, -1, 1, 0, 0, 0, 0};
}
