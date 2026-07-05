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

package com.deathmotion.totemguard.common.physics.preset;

// STRICT pads exist only for genuinely unknowable noise, never to mask a model bug.
public record PhysicsPreset(
        String name,
        double horizontalNoisePad,
        double verticalNoisePad,
        double horizontalFlagEpsilon,
        double verticalFlagEpsilon,
        double phaseCrossTolerance,
        double phaseEmbedTolerance,
        double knockbackPad,
        double residualCarryCap,
        double modelDriftSlack,
        double stepNoiseSlack,
        double fluidDescentSlack,
        int doubleMoveGraceTicks,
        int hoverGraceTicks,
        double hoverMinGap,
        double setbackBufferThreshold) {

    public static final PhysicsPreset STRICT = new PhysicsPreset(
            "strict",
            0.001, 0.001,
            0.002, 0.003,
            0.02, 0.02,
            0.02,
            0.0, 0.0, 0.03,
            0.02,
            3, 4, 0.9,
            1.5);

    public static final PhysicsPreset LENIENT = new PhysicsPreset(
            "lenient",
            0.0025, 0.0025,
            0.002, 0.003,
            0.05, 0.05,
            0.05,
            0.1, 0.03, 0.10,
            0.05,
            3, 6, 0.9,
            8.0);

    public static PhysicsPreset parse(String raw) {
        if (raw == null) return STRICT;
        return switch (raw.trim().toLowerCase()) {
            case "lenient", "standard" -> LENIENT;
            default -> STRICT;
        };
    }
}
