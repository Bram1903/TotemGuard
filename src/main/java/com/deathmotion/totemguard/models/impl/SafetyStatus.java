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

package com.deathmotion.totemguard.models.impl;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public enum SafetyStatus {
    SAFE("Safe", NamedTextColor.GREEN),
    ALERTED("Alerted", NamedTextColor.YELLOW),
    SUSPICIOUS("Suspicious", NamedTextColor.GOLD),
    DANGEROUS("Dangerous", NamedTextColor.RED),
    DIABOLICAL("Diabolical", NamedTextColor.DARK_RED);

    private final String status;
    private final NamedTextColor color;

    SafetyStatus(String status, NamedTextColor color) {
        this.status = status;
        this.color = color;
    }

    public static SafetyStatus getSafetyStatus(int alerts, int punishments) {
        // If no alerts and no punishments, the status is SAFE
        if (alerts == 0 && punishments == 0) {
            return SAFE;
        }

        // Determine safety status based on punishments and alerts
        if (punishments > 2) {
            // Severe case due to high number of punishments
            if (alerts > 7) {
                return DIABOLICAL;
            } else if (alerts > 4) {
                return DANGEROUS;
            } else {
                return SUSPICIOUS;
            }
        } else if (punishments > 0) {
            // Moderate punishments with varying alert levels
            if (alerts > 8) {
                return DIABOLICAL;
            } else if (alerts > 5) {
                return DANGEROUS;
            } else if (alerts > 2) {
                return SUSPICIOUS;
            } else {
                return ALERTED;
            }
        } else {
            // No punishments, evaluate based on alerts only
            if (alerts > 7) {
                return DANGEROUS;
            } else if (alerts > 4) {
                return SUSPICIOUS;
            } else {
                return ALERTED;
            }
        }
    }

    public Component toComponent() {
        return Component.text(status, color);
    }
}
