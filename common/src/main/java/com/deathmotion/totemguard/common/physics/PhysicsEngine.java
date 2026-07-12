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

package com.deathmotion.totemguard.common.physics;

import com.deathmotion.totemguard.common.physics.fall.FallTracker;
import com.deathmotion.totemguard.common.physics.mitigation.MitigationTracker;
import com.deathmotion.totemguard.common.physics.simulation.SelfSimulation;
import com.deathmotion.totemguard.common.physics.simulation.VehicleSimulation;
import com.deathmotion.totemguard.common.physics.trace.TickRecorder;
import com.deathmotion.totemguard.common.physics.trace.TraceRecording;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.jetbrains.annotations.Nullable;

public final class PhysicsEngine {

    private final SelfSimulation self;
    private final VehicleSimulation vehicle;

    public PhysicsEngine(EngineActor actor, Data data, WorldMirror world, EngineContext context) {
        TraceRecording trace = new TraceRecording(actor, data, context);
        this.self = new SelfSimulation(actor, data, world, context, trace);
        this.vehicle = new VehicleSimulation(actor, data, world, context,
                new VersionGates(actor.clientVersion(), actor.supportsEndTick()), trace);
    }

    public void onVehicleMove(double x, double y, double z, float yaw, float pitch) {
        vehicle.onVehicleMove(x, y, z, yaw, pitch);
    }

    public PhysicsVerdict vehicleVerdict() {
        return vehicle.verdict();
    }

    public boolean requestVehicleSetback() {
        return vehicle.requestSetback();
    }

    public void onFlying() {
        self.onFlying();
    }

    public void rewriteGroundClaim(PacketReceiveEvent event) {
        self.rewriteGroundClaim(event);
    }

    public void onTickEnd() {
        self.onTickEnd();
    }

    public void onBlockApplied(int x, int y, int z, int serverStateId) {
        self.onBlockApplied(x, y, z, serverStateId);
    }

    public void onInventoryToggled() {
        self.onInventoryToggled();
    }

    public void clearHistory() {
        self.clearHistory();
    }

    public void reset() {
        self.reset();
        vehicle.reset();
    }

    public PhysicsVerdict verdict() {
        return self.verdict();
    }

    public @Nullable TickRecorder recorder() {
        return self.recorder();
    }

    public boolean dumpTrace(String cause) {
        return self.dumpTrace(cause);
    }

    public MitigationTracker mitigation() {
        return self.mitigation();
    }

    public FallTracker fallTracker() {
        return self.fallTracker();
    }
}
