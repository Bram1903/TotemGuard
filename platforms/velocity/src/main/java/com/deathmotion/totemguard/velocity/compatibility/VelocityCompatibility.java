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

package com.deathmotion.totemguard.velocity.compatibility;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.common.util.CompatibilityLogger;

import java.util.logging.Logger;

public final class VelocityCompatibility {

    private static final TGVersion MIN_VELOCITY_VERSION = TGVersion.fromString("3.5.0-SNAPSHOT");

    private VelocityCompatibility() {
    }

    public static boolean check(final Logger logger, final String detectedVersion) {
        final TGVersion parsed = TGVersion.fromString(detectedVersion);
        if (parsed.isOlderThan(MIN_VELOCITY_VERSION)) {
            logUnsupportedVersion(logger, detectedVersion);
            return false;
        }
        return true;
    }

    private static void logUnsupportedVersion(final Logger logger, final String detected) {
        CompatibilityLogger.severe(logger,
                "TotemGuard does not support this Velocity version.",
                "Minimum supported Velocity version: " + MIN_VELOCITY_VERSION,
                "Detected Velocity version string: " + detected,
                "Please update to a supported version.");
    }
}
