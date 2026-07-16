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

package com.deathmotion.totemguard.common.physics.simulation;

import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.prescan.DeclineCheck;
import com.deathmotion.totemguard.common.physics.prescan.FastDetector;
import com.deathmotion.totemguard.common.physics.prescan.TeleportFilter;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.github.retrooper.packetevents.protocol.world.Location;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class TickGate {

    private static final int SLEEP_ENTER_GRACE = 2;
    private static final int SLEEP_EXIT_GRACE = 3;

    private final TeleportFilter teleportFilter = new TeleportFilter();
    private final FastDetector fastDetector = new FastDetector();
    private boolean lastSleeping;
    private boolean sleepExiting;
    private int sleepGrace;
    @Getter
    private Kind kind = Kind.PROCEED;
    @Getter
    private DeclineReason reason;
    @Getter
    private boolean reseed;
    @Getter
    private CarriedMode carriedMode = CarriedMode.KEEP;
    @Getter
    private BoundBreach breach;
    @Getter
    private double horizontalExcess;
    @Getter
    private double verticalExcess;

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public void evaluateSelf(Data data, WorldMirror world, BlockReader reader, EngineActor actor,
                             MovementData movement, Location current, double observedSpeed) {
        kind = Kind.PROCEED;
        reason = null;
        reseed = false;
        carriedMode = CarriedMode.KEEP;
        breach = null;
        horizontalExcess = 0.0;
        verticalExcess = 0.0;

        boolean sleeping = data.isSleeping();
        if (sleeping != lastSleeping) {
            sleepExiting = !sleeping;
            sleepGrace = sleeping ? SLEEP_ENTER_GRACE : SLEEP_EXIT_GRACE;
        }
        lastSleeping = sleeping;

        switch (teleportFilter.classify(movement, data.getTeleportData(),
                data.getMitigationService().setbackPending())) {
            case RESYNC_REST_PENDING_SETBACK -> {
                decline(DeclineReason.RESYNC, false, CarriedMode.REST);
                return;
            }
            case RESYNC_REST -> {
                decline(DeclineReason.RESYNC, false, CarriedMode.REST_JUMP_CEILING);
                return;
            }
            case RESYNC_PRESERVED -> {
                teleportFilter.startPreserveGrace();
                decline(DeclineReason.RESYNC, false, CarriedMode.FROZEN);
                return;
            }
            case RESYNC_OTHER -> {
                decline(DeclineReason.RESYNC, true, CarriedMode.KEEP);
                return;
            }
            case TELEPORT_PRESERVED -> {
                decline(DeclineReason.TELEPORT, false, CarriedMode.FROZEN);
                return;
            }
            case TELEPORT -> {
                decline(DeclineReason.TELEPORT, true, CarriedMode.KEEP);
                return;
            }
            case NONE -> {
            }
        }

        if (!world.readiness().ready()) {
            int feetChunkX = floor(current.getX()) >> 4;
            int feetChunkZ = floor(current.getZ()) >> 4;
            if (reader.columnLoaded(feetChunkX, feetChunkZ)) {
                world.readiness().requestReadiness(actor.latencyHandler());
            }
            decline(DeclineReason.LOADING, true, CarriedMode.KEEP);
            return;
        }

        if (!movement.isCameraIsSelf()) {
            decline(DeclineReason.CAMERA, true, CarriedMode.KEEP);
            return;
        }
        DeclineReason bail = DeclineCheck.check(data);
        if (bail != null) {
            decline(bail, true, CarriedMode.KEEP);
            return;
        }
        if (sleepGrace > 0) {
            sleepGrace--;
            if (sleepExiting) {
                decline(DeclineReason.SLEEPING, true, CarriedMode.KEEP);
            } else {
                decline(DeclineReason.SLEEPING, false, CarriedMode.REST);
            }
            return;
        }

        double fastCap = FastDetector.cap(data.getAttributeData().movementSpeed());
        switch (fastDetector.evaluate(observedSpeed, fastCap, data.getExternalVelocityData().isActive())) {
            case DECLINE -> {
                decline(DeclineReason.FAST, true, CarriedMode.KEEP);
                return;
            }
            case FLAG -> {
                kind = Kind.FLAG;
                breach = BoundBreach.FAST;
                horizontalExcess = observedSpeed - fastCap;
                verticalExcess = 0.0;
                return;
            }
            case NONE -> {
            }
        }

        if (!reader.columnLoaded(floor(current.getX()) >> 4, floor(current.getZ()) >> 4)) {
            decline(DeclineReason.UNLOADED, true, CarriedMode.KEEP);
        }
    }

    public MotionArea frozen(MotionArea area, double gravity, double jumpCeiling) {
        return teleportFilter.frozen(area, gravity, jumpCeiling);
    }

    public boolean inPreserveGrace() {
        return teleportFilter.inPreserveGrace();
    }

    public void tickPreserveGrace() {
        teleportFilter.tickPreserveGrace();
    }

    public void clearHistory() {
        teleportFilter.reset();
        fastDetector.reset();
        lastSleeping = false;
        sleepExiting = false;
        sleepGrace = 0;
    }

    private void decline(DeclineReason reason, boolean reseed, CarriedMode mode) {
        this.kind = Kind.DECLINE;
        this.reason = reason;
        this.reseed = reseed;
        this.carriedMode = mode;
    }

    public enum Kind {
        PROCEED,
        DECLINE,
        FLAG
    }

    public enum CarriedMode {
        KEEP,
        REST,
        REST_JUMP_CEILING,
        FROZEN
    }
}
