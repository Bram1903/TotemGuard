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

package com.deathmotion.totemguard.bungee.compatibility;

import com.deathmotion.totemguard.common.util.CompatibilityLogger;
import lombok.experimental.UtilityClass;

import java.util.logging.Logger;

@UtilityClass
public final class BungeeCompatibility {

    public boolean check(final Logger logger, final String detectedVersion) {
        if (detectedVersion == null || detectedVersion.isBlank()) {
            CompatibilityLogger.severe(logger,
                    "TotemGuard could not determine the running BungeeCord version.",
                    "Please report this issue if you believe it is a bug.");
            return false;
        }
        // BungeeCord exposes a free-form version string (e.g. "git:BungeeCord-Bootstrap:1.21-R0.1-SNAPSHOT:...")
        // that does not lend itself to semantic parsing, so we currently only sanity-check its presence.
        return true;
    }
}
