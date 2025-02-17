/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

import org.bukkit.entity.Player;

/**
 * Interface for managing alerts.
 */
public interface AlertManager {

    /**
     * Check if alerts are enabled for a player.
     *
     * @param player The player to check.
     * @return True if alerts are enabled, false otherwise.
     */
    boolean hasAlertsEnabled(Player player);

    /**
     * Enable or disable alerts for a player.
     *
     * @param player The player to toggle alerts for.
     * @return
     */
    boolean toggleAlerts(Player player);

    /**
     * Enable alerts for a player.
     *
     * @param player The player to enable alerts for.
     * @return
     */
    boolean enableAlerts(Player player);
}
