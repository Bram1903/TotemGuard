/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.data;

import net.kyori.adventure.text.format.NamedTextColor;

public enum SafetyStatus {
    SAFE,
    ALERTED,
    SUSPICIOUS,
    DANGEROUS,
    DIABOLICAL;

    // Method to get the display name for each status
    public String getName() {
        return switch (this) {
            case SAFE -> "Safe";
            case ALERTED -> "Alerted";
            case SUSPICIOUS -> "Suspicious";
            case DANGEROUS -> "Dangerous";
            case DIABOLICAL -> "Diabolical";
        };
    }

    // Method to get the default NamedTextColor for each status
    public NamedTextColor getColor() {
        return switch (this) {
            case SAFE -> NamedTextColor.GREEN;
            case ALERTED -> NamedTextColor.YELLOW;
            case SUSPICIOUS -> NamedTextColor.GOLD;
            case DANGEROUS -> NamedTextColor.RED;
            case DIABOLICAL -> NamedTextColor.DARK_RED;
        };
    }

    public static SafetyStatus getSafetyStatus(int alerts, int punishments) {
        if (alerts == 0 && punishments == 0) {
            return SafetyStatus.SAFE;
        }

        // Multiple punishments with high alerts indicate a critical state
        if (punishments > 2) {
            if (alerts > 7) {
                return SafetyStatus.DIABOLICAL;
            } else if (alerts > 4) {
                return SafetyStatus.DANGEROUS;
            } else {
                return SafetyStatus.SUSPICIOUS;
            }
        }

        // Some punishments and varying alert levels
        if (punishments > 0) {
            if (alerts > 8) {
                return SafetyStatus.DIABOLICAL;
            } else if (alerts > 5) {
                return SafetyStatus.DANGEROUS;
            } else if (alerts > 2) {
                return SafetyStatus.SUSPICIOUS;
            } else {
                return SafetyStatus.ALERTED;
            }
        }

        // No punishments but alerts are present
        if (alerts > 7) {
            return SafetyStatus.DANGEROUS; // Elevated state due to high alerts even without punishments
        } else if (alerts > 4) {
            return SafetyStatus.SUSPICIOUS;
        } else {
            return SafetyStatus.ALERTED;
        }
    }
}

