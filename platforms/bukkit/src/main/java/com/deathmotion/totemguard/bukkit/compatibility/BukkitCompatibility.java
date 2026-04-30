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

package com.deathmotion.totemguard.bukkit.compatibility;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.common.util.CompatibilityLogger;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

@UtilityClass
public final class BukkitCompatibility {

    private final TGVersion MIN_PAPER_VERSION = TGVersion.fromString("1.16.5");

    private final boolean PAPER_DETECTED =
            hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");

    public boolean check(final Logger logger) {
        if (!PAPER_DETECTED) {
            logUnsupportedServerSoftware(logger);
            return false;
        }

        final String detected = Bukkit.getMinecraftVersion();
        final TGVersion parsed = TGVersion.fromString(detected);
        if (parsed.isOlderThan(MIN_PAPER_VERSION)) {
            logUnsupportedVersion(logger, detected);
            return false;
        }

        return true;
    }

    private boolean hasClass(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private void logUnsupportedServerSoftware(final Logger logger) {
        CompatibilityLogger.severe(logger,
                "TotemGuard cannot run on this server platform.",
                "TotemGuard requires a Paper server or a Paper-based fork to function properly.",
                "Your current server software is not recognized as Paper-compatible.",
                "Please switch to a Paper server to use TotemGuard.");
    }

    private void logUnsupportedVersion(final Logger logger, final String detected) {
        CompatibilityLogger.severe(logger,
                "TotemGuard does not support this Paper version.",
                "Minimum supported Paper version: " + MIN_PAPER_VERSION,
                "Detected Paper version string: " + detected,
                "Please update to a supported version.");
    }
}
