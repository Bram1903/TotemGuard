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

package com.deathmotion.totemguard.common.util;

import com.deathmotion.totemguard.common.TGPlatform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.PEVersion;
import lombok.experimental.UtilityClass;

import java.util.logging.Logger;

/**
 * Common-side compatibility validation. Only checks dependencies that are
 * shared across every platform (currently PacketEvents). Platform-specific
 * checks are owned by each {@link TGPlatform} subclass via
 * {@link TGPlatform#checkPlatformCompatibility()}.
 */
@UtilityClass
public final class CompatibilityUtil {

    private final PEVersion MIN_PE_VERSION = PEVersion.fromString("2.11.1");

    public boolean isCompatible() {
        final TGPlatform platform = TGPlatform.getInstance();

        if (!platform.checkPlatformCompatibility()) {
            return false;
        }

        return checkPacketEvents(platform.getLogger());
    }

    private boolean checkPacketEvents(final Logger logger) {
        if (!TGPlatform.getInstance().isPluginEnabled("packetevents")) {
            CompatibilityLogger.severe(logger,
                    "TotemGuard cannot start because PacketEvents is not loaded or failed to initialize.",
                    "This may happen if PacketEvents crashed on startup or does not support this platform/version.",
                    "Please install a supported version of PacketEvents and ensure it loads correctly.");
            return false;
        }

        final PEVersion current = PacketEvents.getAPI().getVersion();
        if (current.isOlderThan(MIN_PE_VERSION)) {
            CompatibilityLogger.severe(logger,
                    "TotemGuard cannot run with this version of PacketEvents.",
                    "Minimum supported PacketEvents version: " + MIN_PE_VERSION,
                    "Detected PacketEvents version: " + current,
                    "Please update the PacketEvents plugin to a supported version.");
            return false;
        }

        return true;
    }
}
