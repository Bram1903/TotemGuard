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

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum SimulationTolerance {
    STRICT(1.0, 1.5, 0.0, 0.0, 0.03),
    STANDARD(1.5, 4.0, 0.05, 0.015, 0.06),
    LENIENT(2.5, 8.0, 0.1, 0.03, 0.10);

    private final double padScale;
    private final double setbackBuffer;
    private final double residualCarryCap;
    private final double modelDriftSlack;
    private final double stepHorizontalSlack;

    SimulationTolerance(double padScale, double setbackBuffer, double residualCarryCap,
                        double modelDriftSlack, double stepHorizontalSlack) {
        this.padScale = padScale;
        this.setbackBuffer = setbackBuffer;
        this.residualCarryCap = residualCarryCap;
        this.modelDriftSlack = modelDriftSlack;
        this.stepHorizontalSlack = stepHorizontalSlack;
    }

    public static SimulationTolerance parse(String raw) {
        if (raw == null) return STRICT;
        String normalized = raw.trim();
        for (SimulationTolerance value : values()) {
            if (value.name().equalsIgnoreCase(normalized)) return value;
        }
        return STRICT;
    }
}
