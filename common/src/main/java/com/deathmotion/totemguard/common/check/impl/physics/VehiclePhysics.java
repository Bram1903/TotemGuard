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
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.physics.vehicle.VehicleEngine;
import com.deathmotion.totemguard.common.physics.vehicle.VehicleVerdict;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.Map;

@CheckData(description = "Impossible vehicle physics", type = CheckType.PHYSICS, experimental = true)
public class VehiclePhysics extends CheckImpl implements PacketCheck {

    private final VehicleEngine engine;

    public VehiclePhysics(TGPlayer player) {
        super(player);
        this.engine = player.getVehicleEngine();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.VEHICLE_MOVE) return;
        VehicleVerdict verdict = engine.getVerdict();
        if (!verdict.breach()) return;
        fail(Map.of("tg_physics_type", verdict.label()),
                "{0} | observed={1} allowed={2} | over={3}",
                verdict.label(), fmt(verdict.observed()), fmt(verdict.bound()), fmt(verdict.excess()));
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }
}
