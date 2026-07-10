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

package com.deathmotion.totemguard.common.check.impl.physics;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.impl.physics.report.FallReporter;
import com.deathmotion.totemguard.common.check.impl.physics.report.MovementReporter;
import com.deathmotion.totemguard.common.check.impl.physics.report.VehicleReporter;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.physics.PhysicsEngine;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(description = "Impossible physics", type = CheckType.PHYSICS, experimental = true)
public class Physics extends CheckImpl implements PacketCheck {

    private final PhysicsEngine physics;

    private final FallReporter fallReporter;
    private final MovementReporter movementReporter;
    private final VehicleReporter vehicleReporter;

    public Physics(TGPlayer player) {
        super(player);
        this.physics = player.getPhysics();
        this.fallReporter = new FallReporter(this::fail);
        this.movementReporter = new MovementReporter(this::fail, data);
        this.vehicleReporter = new VehicleReporter(this::fail, physics, platform);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            vehicleReporter.report(physics.vehicleVerdict());
            return;
        }

        boolean flying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        boolean tickEnd = event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END;
        if (!flying && !tickEnd) return;

        PhysicsVerdict verdict = physics.verdict();
        if (flying) fallReporter.report(verdict);
        movementReporter.report(verdict);
    }
}
