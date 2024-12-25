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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Checks {

    @Comment("This command placeholder can be used by using %default_punishment% as a punishment command.")
    private String DefaultPunishment = "ban %player% 1d [TotemGuard] Unfair Advantage";

    @Comment("\nAutoTotemA")
    private AutoTotemA AutoTotemA = new AutoTotemA();

    @Comment("\nBadPacketsA")
    private BadPacketsA BadPacketsA = new BadPacketsA();

    @Comment("\nBadPacketsB")
    private BadPacketsB BadPacketsB = new BadPacketsB();

    @Comment("\nBadPacketsC")
    private BadPacketsC BadPacketsC = new BadPacketsC();

    public AbstractCheckSettings getCheckSettings(String checkName) {
        return switch (checkName) {
            case "AutoTotemA" -> AutoTotemA;
            case "BadPacketsA" -> BadPacketsA;
            case "BadPacketsB" -> BadPacketsB;
            case "BadPacketsC" -> BadPacketsC;
            default ->
                    throw new IllegalStateException("Check " + checkName + " does not have a corresponding configuration.");
        };
    }

    @Configuration
    @Getter
    public abstract static class CheckSettings implements AbstractCheckSettings {
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
    public static class AutoTotemA extends CheckSettings {
        @Comment("\nNormal Check Time: Sets the interval (in ms) for normal checks.")
        private int NormalCheckTimeMs = 1500;

        @Comment("\nClick Time Difference: The value (in ms) which anything below will trigger the flag.")
        private int ClickTimeDifference = 75;

        public AutoTotemA() {
            super(true, 2, Component.text("Totem Time: ", NamedTextColor.GOLD).append(Component.text("%totem_time%ms", NamedTextColor.GRAY).append(Component.newline()).append(Component.text("Real Totem Time: ", NamedTextColor.GOLD).append(Component.text("%real_totem_time%ms", NamedTextColor.GRAY).append(Component.newline()).append(Component.text("Click Time Difference: ", NamedTextColor.GOLD).append(Component.text("%click_time_difference%ms", NamedTextColor.GRAY).append(Component.newline()).append(Component.text("Main Hand: ", NamedTextColor.GOLD).append(Component.text("%main_hand%", NamedTextColor.GRAY).append(Component.newline().append(Component.text("States: ", NamedTextColor.GOLD).append(Component.text("%states%", NamedTextColor.GRAY))))))))))));
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsA extends CheckSettings {
        public BadPacketsA() {
            super(true, 20, 1, Component.text("Channel: ", NamedTextColor.GOLD).append(Component.text("%channel%", NamedTextColor.GRAY)));
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsB extends CheckSettings {
        @Comment("\nBanned Client Brands: The list of client brands to flag.")
        private List<String> BannedClientBrands = List.of(
                "autototem"
        );

        public BadPacketsB() {
            super(true, 20, 1, Component.text("Client Brand: ", NamedTextColor.GOLD).append(Component.text("%client_brand%", NamedTextColor.GRAY)));
        }
    }

    @Configuration
    @Getter
    public static class BadPacketsC extends CheckSettings {
        public BadPacketsC() {
            super(true, 20, 3, Component.text("New Slot Change: ", NamedTextColor.GOLD).append(Component.text("%new_slot%", NamedTextColor.GRAY).append(Component.newline()).append(Component.text("Last Slot Change: ", NamedTextColor.GOLD).append(Component.text("%last_slot%", NamedTextColor.GRAY)))));
        }
    }
}
