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

import com.deathmotion.totemguard.interfaces.AbstractCheckSettings;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

import java.util.List;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Checks {

    @Comment("This command placeholder can be used by using %default_punishment% as a punishment command.")
    private String defaultPunishment = "ban %player% 1d [TotemGuard] Unfair Advantage";

    @Comment("\nAutoTotemA")
    private AutoTotemA autoTotemA = new AutoTotemA();

    @Comment("\nBadPacketsA")
    private BadPacketsA badPacketsA = new BadPacketsA();

    @Comment("\nBadPacketsB")
    private BadPacketsB badPacketsB = new BadPacketsB();

    @Comment("\nBadPacketsC")
    private BadPacketsC badPacketsC = new BadPacketsC();

    public AbstractCheckSettings getCheckSettings(String checkName) {
        return switch (checkName) {
            case "AutoTotemA" -> autoTotemA;
            case "BadPacketsA" -> badPacketsA;
            case "BadPacketsB" -> badPacketsB;
            case "BadPacketsC" -> badPacketsC;
            default ->
                    throw new IllegalStateException("Check " + checkName + " does not have a corresponding configuration.");
        };
    }

    @Configuration
    @Getter
    public abstract static class CheckSettings implements AbstractCheckSettings {
        private boolean enabled = true;
        private boolean punishable;
        private int punishmentDelayInSeconds = 0;
        private int maxViolations;
        private List<String> punishmentCommands = List.of(
                "%default_punishment%"
        );

        public CheckSettings(boolean punishable, int punishmentDelay, int maxViolations) {
            this.punishable = punishable;
            this.punishmentDelayInSeconds = punishmentDelay;
            this.maxViolations = maxViolations;
        }

        public CheckSettings(boolean punishable, int maxViolations) {
            this.punishable = punishable;
            this.maxViolations = maxViolations;
        }
    }

    @Configuration
    @Getter
    public static class AutoTotemA extends CheckSettings {
        @Comment("\nNormal Check Time: Sets the interval (in ms) for normal checks.")
        private int normalCheckTimeMs = 1500;

        @Comment("\nClick Time Difference: The value (in ms) which anything below will trigger the flag.")
        private int clickTimeDifference = 75;

        public AutoTotemA() {
            super(true, 2);
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsA extends CheckSettings {
        public BadPacketsA() {
            super(true, 20, 1);
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsB extends CheckSettings {
        @Comment("\nBanned Client Brands: The list of client brands to flag.")
        private List<String> bannedBrands = List.of(
                "autototem"
        );

        public BadPacketsB() {
            super(true, 20, 1);
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsC extends CheckSettings {
        public BadPacketsC() {
            super(true, 20, 3);
        }
    }
}
