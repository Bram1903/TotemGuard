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

public final class FireworkData {

    private static final int MAX_TRACKED = 8;
    private static final int LIFETIME_JITTER = 11;
    private static final int LIFETIME_SLOP = 3;
    private static final int UNKNOWN_MAX_BOOST_TICKS = 51 + 20;
    private static final int BOUNDARY_TICKS = 2;

    private final int[] candidateIds = new int[MAX_TRACKED];
    private final int[] candidateFlight = new int[MAX_TRACKED];
    private final int[] ids = new int[MAX_TRACKED];
    private final int[] ages = new int[MAX_TRACKED];
    private final int[] maxLife = new int[MAX_TRACKED];
    private int candidateCount;
    private int count;
    private int lingerTicks;
    private int boundaryTicks;

    private static int boostWindow(int flightDuration) {
        if (flightDuration < 0) return UNKNOWN_MAX_BOOST_TICKS;
        return 10 * (flightDuration + 1) + LIFETIME_JITTER + LIFETIME_SLOP;
    }

    public void candidate(int entityId) {
        for (int i = 0; i < candidateCount; i++) {
            if (candidateIds[i] == entityId) return;
        }
        if (candidateCount < MAX_TRACKED) {
            candidateIds[candidateCount] = entityId;
            candidateFlight[candidateCount] = -1;
            candidateCount++;
        }
    }

    public boolean isCandidate(int entityId) {
        for (int i = 0; i < candidateCount; i++) {
            if (candidateIds[i] == entityId) return true;
        }
        return false;
    }

    public void setCandidateFlight(int entityId, int flightDuration) {
        for (int i = 0; i < candidateCount; i++) {
            if (candidateIds[i] == entityId) {
                candidateFlight[i] = flightDuration;
                return;
            }
        }
    }

    public void attach(int entityId) {
        int flightDuration = candidateFlightOf(entityId);
        removeCandidate(entityId);
        for (int i = 0; i < count; i++) {
            if (ids[i] == entityId) return;
        }
        if (count < MAX_TRACKED) {
            ids[count] = entityId;
            ages[count] = 0;
            maxLife[count] = boostWindow(flightDuration);
            count++;
            boundaryTicks = BOUNDARY_TICKS;
        }
    }

    public void onRemove(int entityId) {
        removeCandidate(entityId);
        for (int i = 0; i < count; i++) {
            if (ids[i] != entityId) continue;
            count--;
            ids[i] = ids[count];
            ages[i] = ages[count];
            maxLife[i] = maxLife[count];
            lingerTicks = 1;
            boundaryTicks = BOUNDARY_TICKS;
            return;
        }
    }

    public int boostCountMax() {
        int live = liveCount();
        return live + (lingerTicks > 0 ? 1 : 0);
    }

    public int boostCountMin() {
        int live = liveCount();
        return boundaryTicks > 0 ? Math.max(0, live - 1) : live;
    }

    public void tick() {
        if (lingerTicks > 0) lingerTicks--;
        if (boundaryTicks > 0) boundaryTicks--;
        for (int i = 0; i < count; i++) {
            ages[i]++;
        }
    }

    public void reset() {
        candidateCount = 0;
        count = 0;
        lingerTicks = 0;
        boundaryTicks = 0;
    }

    private int liveCount() {
        int live = 0;
        for (int i = 0; i < count; i++) {
            if (ages[i] <= maxLife[i]) live++;
        }
        return live;
    }

    private int candidateFlightOf(int entityId) {
        for (int i = 0; i < candidateCount; i++) {
            if (candidateIds[i] == entityId) return candidateFlight[i];
        }
        return -1;
    }

    private void removeCandidate(int entityId) {
        for (int i = 0; i < candidateCount; i++) {
            if (candidateIds[i] != entityId) continue;
            candidateCount--;
            candidateIds[i] = candidateIds[candidateCount];
            candidateFlight[i] = candidateFlight[candidateCount];
            return;
        }
    }
}
