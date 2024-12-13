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

package com.deathmotion.totemguard.api.interfaces;

import com.deathmotion.totemguard.api.enums.CheckType;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * This interface provides details about a check.
 */
public interface ICheckDetails {

    /**
     * Gets the name of the check.
     *
     * @return the check name.
     */
    String getCheckName();

    /**
     * Gets a description of the check's purpose or functionality.
     *
     * @return the check description.
     */
    String getCheckDescription();

    /**
     * Gets the type of check.
     *
     * @return the check type.
     */
    CheckType getCheckType();

    /**
     * Gets the alert message displayed when a violation is detected.
     *
     * @return the alert message.
     */
    Component getAlert();

    /**
     * Gets additional details about the check, such as its configuration or status.
     *
     * @return the additional details.
     */
    Component getDetails();

    /**
     * Gets the number of violations detected by this check.
     *
     * @return the number of violations.
     */
    int getViolations();

    /**
     * Gets the server's ticks per second (TPS) when the check is triggered.
     *
     * @return the TPS value.
     */
    int getTps();

    /**
     * Gets the player's ping when the check is triggered.
     *
     * @return the player's ping.
     */
    int getPing();

    /**
     * Gets the player's game mode at the time the check is triggered.
     *
     * @return the player's game mode.
     */
    String getGamemode();

    /**
     * Checks if the check is experimental and not fully tested.
     *
     * @return {@code true} if the check is experimental, {@code false} otherwise.
     */
    boolean isExperimental();

    /**
     * Checks if the check is enabled.
     *
     * @return {@code true} if the check is enabled, {@code false} otherwise.
     */
    boolean isEnabled();

    /**
     * Checks if the check's violations can lead to punishment.
     *
     * @return {@code true} if the check is punishable, {@code false} otherwise.
     */
    boolean isPunishable();

    /**
     * Gets the delay (in seconds) before applying punishment after a violation.
     *
     * @return the punishment delay.
     */
    int getPunishmentDelay();

    /**
     * Gets the maximum number of violations allowed before triggering punishment.
     *
     * @return the maximum violations.
     */
    int getMaxViolations();

    /**
     * Gets a list of commands to execute when the player is punished for violating this check.
     *
     * @return the punishment commands.
     */
    List<String> getPunishmentCommands();
}
