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

package com.deathmotion.totemguard.common.replay.retention;

import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.Location;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RetentionKeyframe(
        long openedNanos,
        List<StickyPackets.Entry> sticky,
        List<Entity> entities,
        List<CheckSnapshot> checks,
        BlockJournal journal,
        double playerX,
        double playerY,
        double playerZ,
        boolean positioned,
        @Nullable Location previousLocation,
        boolean onGround
) {

    public static RetentionKeyframe open(TGPlayer player, StickyPackets sticky, long nanos) {
        List<Entity> entities = new ArrayList<>();
        for (Map.Entry<Integer, TrackedEntity> entry : player.getWorldMirror().entities().trackedById().entrySet()) {
            TrackedEntity tracked = entry.getValue();
            if (!tracked.positioned()) continue;
            entities.add(new Entity(entry.getKey(), tracked.type(), tracked.uuid(),
                    tracked.targetX(), tracked.targetY(), tracked.targetZ()));
        }

        var movement = player.getData().getMovementData();
        Location current = movement.getCurrent();
        Location previous = movement.getPrevious();
        boolean positioned = placed(current);

        return new RetentionKeyframe(nanos, sticky.snapshot(), entities,
                player.getCheckManager().getSnapshot(), new BlockJournal(),
                current == null ? 0.0 : current.getX(),
                current == null ? 0.0 : current.getY(),
                current == null ? 0.0 : current.getZ(),
                positioned, placed(previous) ? previous : null, movement.isOnGround());
    }

    private static boolean placed(@Nullable Location location) {
        return location != null
                && (location.getX() != 0.0 || location.getY() != 0.0 || location.getZ() != 0.0);
    }

    public record Entity(int entityId, EntityType type, UUID uuid, double x, double y, double z) {
    }
}
