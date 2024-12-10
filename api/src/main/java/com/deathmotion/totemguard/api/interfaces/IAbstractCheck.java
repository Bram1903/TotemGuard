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

/**
 * Represents an abstract check.
 * This interface defines the basic properties and behaviors of a check,
 * including its name, description, and whether it is experimental.
 */
public interface IAbstractCheck {

    /**
     * Gets the name of the check.
     *
     * @return the name of the check
     */
    String getCheckName();

    /**
     * Gets a brief description of the check.
     *
     * @return the description of the check
     */
    String getDescription();

    /**
     * Indicates whether the check is experimental.
     *
     * @return {@code true} if the check is experimental; {@code false} otherwise
     */
    boolean isExperimental();
}
