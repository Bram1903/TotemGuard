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

    @Comment("\nAutoTotemB")
    private AutoTotemB autoTotemB = new AutoTotemB();

    @Comment("\nAutoTotemC")
    private AutoTotemC autoTotemC = new AutoTotemC();

    @Comment("\nAutoTotemD")
    private AutoTotemD autoTotemD = new AutoTotemD();

    @Comment("\nAutoTotemE")
    private AutoTotemE autoTotemE = new AutoTotemE();

    @Comment("\nAutoTotemF")
    private AutoTotemF autoTotemF = new AutoTotemF();

    @Comment("\nBadPacketsA")
    private BadPacketsA badPacketsA = new BadPacketsA();

    @Comment("\nBadPacketsB")
    private BadPacketsB badPacketsB = new BadPacketsB();

    @Comment("\nBadPacketsC")
    private BadPacketsC badPacketsC = new BadPacketsC();

    @Comment("\nManualTotemA")
    private ManualTotemA manualTotemA = new ManualTotemA();

    public AbstractCheckSettings getCheckSettings(String checkName) {
        return switch (checkName) {
            case "AutoTotemA" -> autoTotemA;
            case "AutoTotemB" -> autoTotemB;
            case "AutoTotemC" -> autoTotemC;
            case "AutoTotemD" -> autoTotemD;
            case "AutoTotemE" -> autoTotemE;
            case "AutoTotemF" -> autoTotemF;
            case "BadPacketsA" -> badPacketsA;
            case "BadPacketsB" -> badPacketsB;
            case "BadPacketsC" -> badPacketsC;
            case "ManualTotemA" -> manualTotemA;
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
    public static class AutoTotemB extends CheckSettings {
        @Comment("\nStandard Deviation Threshold: The threshold for the standard deviation.")
        private double StandardDeviationThreshold = 30.0;

        @Comment("\nMean Threshold: The threshold for the mean.")
        private double MeanThreshold = 500.0;

        @Comment("\nConsecutive Low SD Count: The amount of consecutive low standard deviations before flagging.")
        private int ConsecutiveLowSDCount = 3;

        public AutoTotemB() {
            super(true, 6);
        }
    }

    @Configuration
    @Getter
    public static class AutoTotemC extends CheckSettings {
        @Comment("\nConsistent SD Range: The range for the standard average deviation.")
        private double ConsistentSDRange = 1.0;

        @Comment("\nConsecutive Violations: The amount of consecutive violations before flagging.")
        private int ConsecutiveViolations = 3;

        public AutoTotemC() {
            super(true, 3);
        }
    }

    @Configuration
    @Getter
    public static class AutoTotemD extends CheckSettings {
        @Comment("\nTotal Sequence: The total sequence timing under which the player will be flagged.")
        private int TotalSequence = 160;

        @Comment("\nTime average Difference between packets: The time difference between packets.")
        private int BaseTimeDifference = 50;

        @Comment("\nTime Tolerance: The tolerance for the time difference.")
        private int Tolerance = 5;

        public AutoTotemD() {
            super(true, 2);
        }
    }

    @Configuration
    @Getter
    public static class AutoTotemE extends CheckSettings {
        @Comment("\nStandard Deviation Threshold: The threshold for the standard deviation.")
        private double StandardDeviationThreshold = 10.0;

        @Comment("\nAverage Standard Deviation Threshold: The threshold for the average standard deviation.")
        private double AverageStDeviationThreshold = 10.0;

        public AutoTotemE() {
            super(true, 4);
        }
    }

    @Configuration
    @Getter
    public static class AutoTotemF extends CheckSettings {
        @Comment("\nTime Difference: The time difference between closing the inventory and the last click.")
        private int TimeDifference = 1500;

        public AutoTotemF() {
            super(false, 6);
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

    @Configuration
    @Getter
    public static class ManualTotemA extends CheckSettings {
        @Comment("\nCheck Time: Amount of time the check command waits for a retotem. (in ms)")
        private int CheckTime = 400;

        public ManualTotemA() {
            super(false, 4);
        }
    }
}
