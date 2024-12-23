/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.config;

import com.deathmotion.totemguard.interfaces.ICheckSettings;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Checks {

    @Comment("This command placeholder can be used by using %default_punishment% as a punishment command.")
    private String DefaultPunishment = "ban %player% 1d [TotemGuard] Unfair Advantage";

    @Comment("\nBadPacketsA")
    private BadPacketsA BadPacketsA = new BadPacketsA();

    public ICheckSettings getCheckSettings(String checkName) {
        return switch (checkName) {
            case "BadPacketsA" -> BadPacketsA;
            default -> throw new IllegalStateException("Check " + checkName + " does not have a corresponding configuration.");
        };
    }

    @Configuration
    @Getter
    public abstract static class CheckSettings implements ICheckSettings {
        private boolean Enabled = true;
        private boolean Punishable;
        private int PunishmentDelayInSeconds = 0;
        private int MaxViolations;
        private Component CheckAlertMessage;
        private List<String> PunishmentCommands = List.of(
                "%default_punishment%"
        );

        public CheckSettings(boolean punishable, int punishmentDelay, int maxViolations, Component checkAlertMessage) {
            this.Punishable = punishable;
            this.PunishmentDelayInSeconds = punishmentDelay;
            this.MaxViolations = maxViolations;
            this.CheckAlertMessage = checkAlertMessage;
        }

        public CheckSettings(boolean punishable, int maxViolations, Component checkAlertMessage) {
            this.Punishable = punishable;
            this.MaxViolations = maxViolations;
            this.CheckAlertMessage = checkAlertMessage;
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsA extends CheckSettings {
        public BadPacketsA() {
            super(true, 20, 1, Component.text("Channel: ", NamedTextColor.GOLD).append(Component.text("%channel%", NamedTextColor.GRAY)));
        }
    }
}
