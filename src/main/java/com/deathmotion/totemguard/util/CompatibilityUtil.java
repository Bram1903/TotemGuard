/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.TotemGuard;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.PEVersion;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

public final class CompatibilityUtil {

    private static final boolean IS_PAPER = hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");
    private static final ServerVersion MIN_SERVER_VERSION = ServerVersion.V_1_16_5;
    private static final PEVersion MIN_PE_VERSION = PEVersion.fromString("2.10.0");

    public static void init() {
        // Just to make sure the ServerVersion class gets loaded before PacketEvents disables itself for incompatibility
    }

    private static boolean hasClass(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean detectServerVersionSupported() {
        final String bukkitVersion = Bukkit.getBukkitVersion();
        ServerVersion detected = ServerVersion.ERROR;

        for (final ServerVersion candidate : ServerVersion.reversedValues()) {
            final String release = candidate.getReleaseName();
            if (bukkitVersion.contains(release)) {
                detected = candidate;
                break;
            }
        }
        return detected != ServerVersion.ERROR && !detected.isOlderThan(MIN_SERVER_VERSION);
    }

    private static boolean detectPacketEventsSupported() {
        final PEVersion current = PacketEvents.getAPI().getVersion();
        return !current.isOlderThan(MIN_PE_VERSION);
    }

    private static void logUnsupportedPlatform() {
        final Logger logger = TotemGuard.getInstance().getLogger();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard cannot run on this server platform.");
        logger.severe(" TotemGuard requires a Paper server or a Paper-based fork to function properly.");
        logger.severe(" Your current server software is not recognized as Paper-compatible.");
        logger.severe(" Please switch to a Paper server to use TotemGuard.");
        logger.severe("=====================================================");
    }

    private static void logUnsupportedServerVersion() {
        final Logger logger = TotemGuard.getInstance().getLogger();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard does not support this Minecraft server version.");
        logger.severe(" Minimum supported version: " + MIN_SERVER_VERSION.getReleaseName());
        logger.severe(" Detected server version: " + Bukkit.getMinecraftVersion());
        logger.severe(" Please update your server to a supported version.");
        logger.severe("=====================================================");
    }

    private static void logPacketEventsNotLoaded() {
        final Logger logger = TotemGuard.getInstance().getLogger();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard cannot start because PacketEvents is not loaded or failed to initialize.");
        logger.severe(" This may happen if PacketEvents crashed on startup or does not support this Minecraft version.");
        logger.severe(" Please install a supported version of PacketEvents and ensure it loads correctly.");
        logger.severe("=====================================================");
    }

    private static void logUnsupportedPEVersion() {
        final Logger logger = TotemGuard.getInstance().getLogger();
        final PEVersion current = PacketEvents.getAPI().getVersion();
        logger.severe("=====================================================");
        logger.severe(" TotemGuard cannot run with this version of PacketEvents.");
        logger.severe(" Minimum supported PacketEvents version: " + MIN_PE_VERSION);
        logger.severe(" Detected PacketEvents version: " + current);
        logger.severe(" Please update the PacketEvents plugin to a supported version.");
        logger.severe("=====================================================");
    }

    public static boolean isCompatible() {
        if (!IS_PAPER) {
            logUnsupportedPlatform();
            return false;
        }
        if (!detectServerVersionSupported()) {
            logUnsupportedServerVersion();
            return false;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
            logPacketEventsNotLoaded();
            return false;
        }
        if (!detectPacketEventsSupported()) {
            logUnsupportedPEVersion();
            return false;
        }
        return true;
    }
}
