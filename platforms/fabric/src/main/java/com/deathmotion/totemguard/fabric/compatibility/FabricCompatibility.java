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

package com.deathmotion.totemguard.fabric.compatibility;

import com.deathmotion.totemguard.api3.versioning.TGVersion;
import com.deathmotion.totemguard.common.util.CompatibilityLogger;
import lombok.experimental.UtilityClass;

import java.util.logging.Logger;

@UtilityClass
public final class FabricCompatibility {

    private final TGVersion MIN_MINECRAFT_VERSION = TGVersion.fromString("26.1.0");

    public boolean check(final Logger logger, final String detectedVersion) {
        final TGVersion parsed = TGVersion.fromString(detectedVersion);
        if (parsed.isOlderThan(MIN_MINECRAFT_VERSION)) {
            CompatibilityLogger.severe(logger,
                    "TotemGuard does not support this Minecraft version on Fabric.",
                    "Minimum supported Minecraft version: " + MIN_MINECRAFT_VERSION,
                    "Detected Minecraft version string: " + detectedVersion,
                    "Please update to a supported version.");
            return false;
        }
        return true;
    }
}
