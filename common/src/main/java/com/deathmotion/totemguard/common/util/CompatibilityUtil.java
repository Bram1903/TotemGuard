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

import com.deathmotion.totemguard.api3.versioning.TGVersion;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.PEVersion;
import lombok.experimental.UtilityClass;

import java.util.logging.Logger;

@UtilityClass
public final class CompatibilityUtil {

    private final boolean IS_PAPER = hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");

    private final TGVersion MIN_PAPER_VER = TGVersion.fromString("1.16.5");
    private final TGVersion MIN_VELOCITY_VER = TGVersion.fromString("3.4.0-SNAPSHOT");

    private final PEVersion MIN_PE_VERSION = PEVersion.fromString("2.11.1");

    private boolean hasClass(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public boolean isCompatible() {
        if (TGPlatform.getInstance().getPlatform() == Platform.PAPER && !IS_PAPER) {
            logUnsupportedPlatform();
            return false;
        }

        if (!detectPlatformVersionSupported()) {
            logUnsupportedPlatformVersion();
            return false;
        }

        if (!TGPlatform.getInstance().isPluginEnabled("packetevents")) {
            logPacketEventsNotLoaded();
            return false;
        }

        if (!detectPacketEventsSupported()) {
            logUnsupportedPEVersion();
            return false;
        }

        return true;
    }

    private boolean detectPlatformVersionSupported() {
        final Platform platform = TGPlatform.getInstance().getPlatform();
        return switch (platform) {
            case PAPER -> detectPaperVersionSupported();
            case VELOCITY -> detectVelocityVersionSupported();
            default -> false;
        };
    }

    private boolean detectPaperVersionSupported() {
        final String mcVersion = TGPlatform.getInstance().getPlatformVersion();
        final TGVersion detected = TGVersion.fromString(mcVersion);
        return !detected.isOlderThan(MIN_PAPER_VER);
    }

    private boolean detectVelocityVersionSupported() {
        final String velocityVersion = TGPlatform.getInstance().getPlatformVersion();
        final TGVersion detected = TGVersion.fromString(velocityVersion);
        return !detected.isOlderThan(MIN_VELOCITY_VER);
    }

    private boolean detectPacketEventsSupported() {
        final PEVersion current = PacketEvents.getAPI().getVersion();
        return !current.isOlderThan(MIN_PE_VERSION);
    }

    private void logUnsupportedPlatform() {
        final Logger logger = TGPlatform.getInstance().getLogger();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard cannot run on this server platform.");
        logger.severe(" TotemGuard requires a Paper server or a Paper-based fork to function properly.");
        logger.severe(" Your current server software is not recognized as Paper-compatible.");
        logger.severe(" Please switch to a Paper server to use TotemGuard.");
        logger.severe("=====================================================");
    }

    private void logUnsupportedPlatformVersion() {
        final Logger logger = TGPlatform.getInstance().getLogger();
        final Platform platform = TGPlatform.getInstance().getPlatform();
        final String detected = TGPlatform.getInstance().getPlatformVersion();

        logger.severe("=====================================================");
        logger.severe(" TotemGuard does not support this platform version.");

        switch (platform) {
            case PAPER -> {
                logger.severe(" Minimum supported Paper version: " + MIN_PAPER_VER);
                logger.severe(" Detected Paper version string: " + detected);
            }
            case VELOCITY -> {
                logger.severe(" Minimum supported Velocity version: " + MIN_VELOCITY_VER);
                logger.severe(" Detected Velocity version string: " + detected);
            }
            default -> logger.severe(" Unsupported platform: " + platform);
        }

        logger.severe(" Please update to a supported version.");
        logger.severe("=====================================================");
    }

    private void logPacketEventsNotLoaded() {
        final Logger logger = TGPlatform.getInstance().getLogger();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard cannot start because PacketEvents is not loaded or failed to initialize.");
        logger.severe(" This may happen if PacketEvents crashed on startup or does not support this platform/version.");
        logger.severe(" Please install a supported version of PacketEvents and ensure it loads correctly.");
        logger.severe("=====================================================");
    }

    private void logUnsupportedPEVersion() {
        final Logger logger = TGPlatform.getInstance().getLogger();
        final PEVersion current = PacketEvents.getAPI().getVersion();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard cannot run with this version of PacketEvents.");
        logger.severe(" Minimum supported PacketEvents version: " + MIN_PE_VERSION);
        logger.severe(" Detected PacketEvents version: " + current);
        logger.severe(" Please update the PacketEvents plugin to a supported version.");
        logger.severe("=====================================================");
    }
}
