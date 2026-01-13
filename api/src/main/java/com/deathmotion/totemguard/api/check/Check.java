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

package com.deathmotion.totemguard.api.check;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single check within TotemGuard.
 * <p>
 * Checks are responsible for identifying suspicious or invalid behavior
 * and may trigger flag events when their conditions are met.
 */
public interface Check {

    /**
     * Gets the unique, human-readable name of this check.
     * <p>
     * This name is typically used for logging, debugging, and
     * administrative output.
     *
     * @return the check name
     */
    @NotNull String getName();

    /**
     * Gets a short description explaining what this check detects.
     *
     * @return the check description
     */
    @NotNull String getDescription();

    /**
     * Gets the category of this check.
     * <p>
     * For example, auto totem, inventory, protocol, etc
     *
     * @return the check category
     */
    @NotNull CheckType getType();

    /**
     * Indicates whether this check is considered experimental.
     * <p>
     * Experimental checks may be incomplete, unstable, or subject to change,
     * and should typically not be enabled by default in production environments.
     *
     * @return {@code true} if the check is experimental, {@code false} otherwise
     */
    boolean isExperimental();

    /**
     * Indicates whether this check is currently enabled.
     *
     * @return {@code true} if the check is enabled, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Gets the current number of violations recorded by this check.
     *
     * @return the number of violations
     */
    int getViolations();
}
