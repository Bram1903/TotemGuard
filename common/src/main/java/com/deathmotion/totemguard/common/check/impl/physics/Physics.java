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
import com.deathmotion.totemguard.common.player.movement.MovementCause;
import com.deathmotion.totemguard.common.player.movement.MovementEstimator;
import com.deathmotion.totemguard.common.player.movement.MovementResult;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.Map;

@CheckData(description = "Impossible physics", type = CheckType.PHYSICS, experimental = true)
public class Physics extends CheckImpl implements PacketCheck {

    private static final double FLAG_THRESHOLD = 5.0;
    private static final double BASE_GAIN = 1.0;
    private static final double EXCESS_GAIN = 12.0;
    private static final double DECAY_FACTOR = 0.92;
    private static final double DECAY_SUBTRACT = 0.30;
    private static final double RETAIN_AFTER_FLAG = 0.5;

    private final MovementEstimator estimator;

    private double violationLevel;

    public Physics(TGPlayer player) {
        super(player);
        this.estimator = player.getData().getMovementEstimator();
    }

    private static String classify(MovementCause cause, boolean verticalDominant, boolean improperSprint,
                                   boolean sneaking) {
        if (cause == MovementCause.GROUNDSPOOF) return "groundspoof";
        if (cause == MovementCause.HOVER) return "hover";
        if (cause == MovementCause.FAST_FALL) return "fastfall";
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
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        MovementResult r = estimator.getResult();
        boolean ascending = r.ascendingThisTick();
        boolean moved = r.movedThisTick();
        if (!ascending && !moved) {
            violationLevel = Math.max(0.0, violationLevel * DECAY_FACTOR - DECAY_SUBTRACT);
            return;
        }

        double vertical = ascending ? r.verticalExcess() : 0.0;
        double horizontal = moved ? r.horizontalExcess() : 0.0;
        double excess = Math.max(vertical, horizontal);

        violationLevel += BASE_GAIN + excess * EXCESS_GAIN;
        if (violationLevel < FLAG_THRESHOLD) return;

        violationLevel = FLAG_THRESHOLD * RETAIN_AFTER_FLAG;

        boolean verticalDominant = vertical >= horizontal;
        String type = classify(r.cause(), verticalDominant, estimator.isImproperSprint(),
                player.getData().isSneaking());
        Map<String, Object> extras = Map.of("tg_physics_type", type);

        if (r.cause() == MovementCause.GROUNDSPOOF) {
            fail(extras, "{0} | claims onGround, vy={1} | over={2}", type, fmt(r.observed().getY()), fmt(excess));
        } else if (r.cause() == MovementCause.HOVER) {
            fail(extras, "{0} | airborne, not falling | over={1}", type, fmt(excess));
        } else if (r.cause() == MovementCause.FAST_FALL) {
            fail(extras, "{0} | vy={1} fell faster than gravity | over={2}",
                    type, fmt(r.observed().getY()), fmt(excess));
        } else if (verticalDominant) {
            fail(extras, "{0} | vy={1} allowed<={2} | over={3}",
                    type, fmt(r.observed().getY()), fmt(r.predicted().vertical().max()), fmt(excess));
        } else {
            double speed = Math.hypot(r.observed().getX(), r.observed().getZ());
            fail(extras, "{0} | speed={1} allowed<={2} | over={3}",
                    type, fmt(speed), fmt(r.predicted().horizontalSpeed().max()), fmt(excess));
        }
    }
}
