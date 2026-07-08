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
import com.deathmotion.totemguard.common.physics.PhysicsEngine;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.Map;

@CheckData(description = "Impossible physics", type = CheckType.PHYSICS, experimental = true)
public class Physics extends CheckImpl implements PacketCheck {

    private final PhysicsEngine physics;

    public Physics(TGPlayer player) {
        super(player);
        this.physics = player.getPhysics();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        boolean flying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        boolean tickEnd = event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END;
        if (!flying && !tickEnd) return;

        PhysicsVerdict verdict = physics.verdict();

        if (flying && verdict.fall().violation()) {
            String shownType = verdict.fall().damageApplied() ? "nofall (damaged)" : "nofall";
            fail(Map.of("tg_physics_type", "nofall"),
                    "{0} | fell {1} blocks, dodged {2} damage", shownType,
                    String.format("%.1f", verdict.fall().fallDistance()),
                    String.format("%.1f", verdict.fall().avoidedDamage()));
        }

        if (!verdict.mitigation().triggered() || verdict.breach() == null) return;

        double excess = Math.max(Math.max(verdict.horizontalExcess(), verdict.ascentExcess()),
                Math.max(verdict.descentExcess(), verdict.phaseExcess()));
        String type = classify(verdict);
        Map<String, Object> extras = Map.of("tg_physics_type", type);
        String shownType = type;
        if (verdict.mitigation().setbackIssued()) {
            shownType = type + " (setback)";
        } else if (verdict.mitigation().setbackSkipped()) {
            shownType = type + " (setback off)";
        }

        switch (verdict.breach()) {
            case GROUNDSPOOF -> fail(extras, "{0} | claims onGround, vy={1} | over={2}",
                    shownType, fmt(verdict.observedY()), fmt(excess));
            case HOVER -> fail(extras, "{0} | airborne, not falling | over={1}", shownType, fmt(excess));
            case DESCENT_FLOOR -> fail(extras, "{0} | vy={1} fell faster than gravity | over={2}",
                    shownType, fmt(verdict.observedY()), fmt(excess));
            case PHASE_CROSS, PHASE_EMBED -> fail(extras, "{0} | moved {1} through a wall | over={2}",
                    shownType, fmt(verdict.observedSpeed()), fmt(excess));
            case FAST -> fail(extras, "{0} | speed={1} | over={2}",
                    shownType, fmt(verdict.observedSpeed()), fmt(excess));
            case ASCENT -> fail(extras, "{0} | vy={1} allowed<={2} | over={3}",
                    shownType, fmt(verdict.observedY()), fmt(verdict.boundCeiling()), fmt(excess));
            case HORIZONTAL_DISK -> fail(extras, "{0} | speed={1} allowed<={2} | over={3}",
                    shownType, fmt(verdict.observedSpeed()),
                    fmt(Math.hypot(verdict.boundCenterX(), verdict.boundCenterZ()) + verdict.boundRadius()),
                    fmt(excess));
        }
    }

    private String classify(PhysicsVerdict verdict) {
        return switch (verdict.breach()) {
            case GROUNDSPOOF -> "groundspoof";
            case HOVER -> "hover";
            case DESCENT_FLOOR -> "fastfall";
            case PHASE_CROSS, PHASE_EMBED -> "phase";
            case FAST -> "speed";
            case ASCENT -> switch (verdict.medium()) {
                case WATER, LAVA -> "water-fly";
                case CLIMB -> "climb-fly";
                case GLIDE -> "glide-fly";
                default -> "fly";
            };
            case HORIZONTAL_DISK -> {
                if (verdict.inventoryOpen()) yield "inventory-move";
                if (verdict.improperSprint()) yield "sprint";
                if (verdict.medium() == MediumKind.WATER || verdict.medium() == MediumKind.LAVA) yield "water-speed";
                if (verdict.medium() == MediumKind.GLIDE) yield "glide-speed";
                yield player.getData().isSneaking() ? "nosneak" : "speed";
            }
        };
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }
}
