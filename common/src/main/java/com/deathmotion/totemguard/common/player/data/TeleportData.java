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

package com.deathmotion.totemguard.common.player.data;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TeleportData {

    private final Set<Integer> pendingTeleportIds = new LinkedHashSet<>();

    private boolean lastPacketWasTeleport;
    private boolean lastTeleportConfirmValid;
    private boolean lastTeleportConfirmSkipped;

    @Getter
    private int lastSkippedTeleportCount;

    public void trackTeleport(int teleportId) {
        pendingTeleportIds.add(teleportId);
    }

    public TeleportConfirmResult validateTeleportConfirm(int teleportId) {
        lastTeleportConfirmValid = false;
        lastTeleportConfirmSkipped = false;
        lastSkippedTeleportCount = 0;

        List<Integer> skippedTeleportIds = new ArrayList<>();
        boolean matched = false;

        for (int pendingTeleportId : pendingTeleportIds) {
            if (pendingTeleportId == teleportId) {
                matched = true;
                break;
            }

            skippedTeleportIds.add(pendingTeleportId);
        }

        if (!matched) {
            return new TeleportConfirmResult(false, teleportId, List.of());
        }

        lastTeleportConfirmValid = true;
        lastTeleportConfirmSkipped = !skippedTeleportIds.isEmpty();

        // Teleport confirms should arrive in the same order teleports were sent. If the client
        // confirms a newer teleport first, every older pending sync was skipped and is now stale.
        if (lastTeleportConfirmSkipped) {
            lastSkippedTeleportCount = skippedTeleportIds.size();
            skippedTeleportIds.forEach(pendingTeleportIds::remove);
        }

        pendingTeleportIds.remove(teleportId);

        lastPacketWasTeleport = true;
        return new TeleportConfirmResult(true, teleportId, List.copyOf(skippedTeleportIds));
    }

    public boolean lastPacketWasTeleport() {
        return lastPacketWasTeleport;
    }

    public boolean lastTeleportConfirmValid() {
        return lastTeleportConfirmValid;
    }

    public boolean lastTeleportConfirmSkipped() {
        return lastTeleportConfirmSkipped;
    }

    public void clearLastPacketWasTeleport() {
        lastPacketWasTeleport = false;
    }

    public record TeleportConfirmResult(boolean valid, int teleportId, List<Integer> skippedTeleportIds) {
    }
}
