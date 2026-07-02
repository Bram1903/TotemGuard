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
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.physics.MovementCause;
import com.deathmotion.totemguard.common.physics.MovementEstimator;
import com.deathmotion.totemguard.common.physics.MovementResult;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.Map;

@CheckData(description = "Impossible physics", type = CheckType.PHYSICS, experimental = true)
public class Physics extends CheckImpl implements PacketCheck {

    private final MovementEstimator estimator;

    public Physics(TGPlayer player) {
        super(player);
        this.estimator = player.getData().getMovementEstimator();
    }

    private static String classify(MovementCause cause, boolean verticalDominant, boolean improperSprint,
                                   boolean sneaking) {
        if (cause == MovementCause.GROUNDSPOOF) return "groundspoof";
        if (cause == MovementCause.HOVER) return "hover";
        if (cause == MovementCause.FAST_FALL) return "fastfall";
        if (cause == MovementCause.PHASE) return "phase";
        if (cause == MovementCause.INVENTORY_MOVE) return "inventory-move";
        if (verticalDominant) {
            return switch (cause) {
                case FLUID -> "water-fly";
                case CLIMB -> "climb-fly";
                default -> "fly";
            };
        }
        if (improperSprint) return "sprint";
        return switch (cause) {
            case STUCK -> "noslow";
            case FLUID -> "water-speed";
            default -> sneaking ? "nosneak" : "speed";
        };
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        boolean flying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        boolean tickEnd = event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END;
        if (!flying && !tickEnd) return;

        if (flying && estimator.fallViolationThisTick()) {
            String shownType = estimator.fallDamageApplied() ? "nofall (damaged)" : "nofall";
            fail(Map.of("tg_physics_type", "nofall"),
                    "{0} | fell {1} blocks, dodged {2} damage", shownType,
                    String.format("%.1f", estimator.fallDistance()),
                    String.format("%.1f", estimator.fallAvoidedDamage()));
        }

        if (!estimator.mitigationTriggeredThisTick()) return;

        MovementResult r = estimator.getResult();
        double vertical = r.ascendingThisTick() ? r.verticalExcess() : 0.0;
        double horizontal = r.movedThisTick() ? r.horizontalExcess() : 0.0;
        double excess = Math.max(vertical, horizontal);
        boolean setback = estimator.setbackIssuedThisTick();

        boolean verticalDominant = vertical >= horizontal;
        String type = classify(r.cause(), verticalDominant, estimator.isImproperSprint(),
                player.getData().isSneaking());
        Map<String, Object> extras = Map.of("tg_physics_type", type);
        String shownType = type;
        if (setback) {
            shownType = type + " (setback)";
        } else if (estimator.setbackSkippedThisTick()) {
            shownType = type + " (setback off)";
        }

        if (r.cause() == MovementCause.GROUNDSPOOF) {
            fail(extras, "{0} | claims onGround, vy={1} | over={2}", shownType, fmt(r.observed().getY()), fmt(excess));
        } else if (r.cause() == MovementCause.HOVER) {
            fail(extras, "{0} | airborne, not falling | over={1}", shownType, fmt(excess));
        } else if (r.cause() == MovementCause.FAST_FALL) {
            fail(extras, "{0} | vy={1} fell faster than gravity | over={2}",
                    shownType, fmt(r.observed().getY()), fmt(excess));
        } else if (r.cause() == MovementCause.PHASE) {
            double speed = Math.hypot(r.observed().getX(), r.observed().getZ());
            fail(extras, "{0} | moved {1} through a wall | over={2}", shownType, fmt(speed), fmt(excess));
        } else if (verticalDominant) {
            fail(extras, "{0} | vy={1} allowed<={2} | over={3}",
                    shownType, fmt(r.observed().getY()), fmt(r.predicted().vertical().max()), fmt(excess));
        } else {
            double speed = Math.hypot(r.observed().getX(), r.observed().getZ());
            fail(extras, "{0} | speed={1} allowed<={2} | over={3}",
                    shownType, fmt(speed), fmt(r.predicted().horizontalSpeed().max()), fmt(excess));
        }
    }
}
