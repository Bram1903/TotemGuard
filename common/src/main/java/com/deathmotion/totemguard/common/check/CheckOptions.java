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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.ConfigSection;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.punishment.PunishmentCommand;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
public class CheckOptions {

    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_PUNISHABLE = false;
    private static final boolean DEFAULT_MITIGATE = false;
    private static final int DEFAULT_MAX_VIOLATIONS = 1;
    private static final List<String> DEFAULT_PUNISH_COMMANDS_RAW = List.of("%default_punishment%");

    // Only these checks actually act on "mitigate" (close-window mitigation in CheckImpl#failInventory).
    // Keeping the allowlist here avoids silently surfacing a config key that would be a no-op elsewhere.
    private static final Set<String> MITIGATION_CAPABLE = Set.of("InventoryA", "InventoryB");

    private final String checkName;
    private final boolean enabled;
    private final boolean punishable;
    private final boolean mitigate;
    private final int maxViolations;
    private final List<PunishmentCommand> punishCommands;

    public CheckOptions(String checkName) {
        this.checkName = checkName;

        Config config = TGPlatform.getInstance()
                .getConfigRepository()
                .config(ConfigFile.CHECKS);

        Optional<ConfigSection> optionalSection = config.getSection(checkName);

        if (optionalSection.isEmpty()) {
            if (!checkName.equals("Mod")) {
                TGPlatform.getInstance().getLogger().warning(
                        "Missing check configuration section for '" + checkName + "' in " + ConfigFile.CHECKS + ". Using fallback values."
                );
            }

            this.enabled = DEFAULT_ENABLED;
            this.punishable = DEFAULT_PUNISHABLE;
            this.mitigate = DEFAULT_MITIGATE;
            this.maxViolations = DEFAULT_MAX_VIOLATIONS;
            this.punishCommands = parsePunishCommands(DEFAULT_PUNISH_COMMANDS_RAW);
            return;
        }

        ConfigSection section = optionalSection.get();

        this.enabled = readBoolean(section, "enabled", DEFAULT_ENABLED);
        this.punishable = readBoolean(section, "punishable", DEFAULT_PUNISHABLE);
        this.mitigate = readMitigate(section, checkName);
        this.maxViolations = Math.max(1, readInt(section, "max-violations", DEFAULT_MAX_VIOLATIONS));
        this.punishCommands = parsePunishCommands(
                readStringList(section, "punishment-commands", DEFAULT_PUNISH_COMMANDS_RAW)
        );
    }

    private static List<PunishmentCommand> parsePunishCommands(List<String> raw) {
        return raw.stream().map(PunishmentCommand::parse).toList();
    }

    private boolean readMitigate(ConfigSection section, String checkName) {
        if (!MITIGATION_CAPABLE.contains(checkName)) return DEFAULT_MITIGATE;
        return readBoolean(section, "mitigate", DEFAULT_MITIGATE);
    }

    private boolean readBoolean(ConfigSection section, String path, boolean defaultValue) {
        Optional<Boolean> value = section.getBoolean(path);

        if (value.isEmpty()) {
            logFallback(path, String.valueOf(defaultValue));
            return defaultValue;
        }

        return value.get();
    }

    private int readInt(ConfigSection section, String path, int defaultValue) {
        Optional<Integer> value = section.getInt(path);

        if (value.isEmpty()) {
            logFallback(path, String.valueOf(defaultValue));
            return defaultValue;
        }

        return value.get();
    }

    private List<String> readStringList(ConfigSection section, String path, List<String> defaultValue) {
        if (!section.contains(path)) {
            logFallback(path, defaultValue.toString());
            return defaultValue;
        }

        List<String> value = section.getStringList(path);
        return List.copyOf(value);
    }

    private void logFallback(String path, String defaultValue) {
        if (checkName.equals("Mod")) {
            return;
        }

        TGPlatform.getInstance().getLogger().warning(
                "Missing check config value '" + checkName + "." + path + "' in " + ConfigFile.CHECKS
                        + ". Falling back to default value: " + defaultValue
        );
    }
}
