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

package com.deathmotion.totemguard.api.alert;

import com.deathmotion.totemguard.api.user.TGUser;

public interface AlertManager {

    /*
     * Checks if alerts are enabled for the specified user
     *
     * @param user The user to check
     */
    boolean hasAlertsEnabled(TGUser user);

    /*
     * Sets whether alerts are enabled for the specified user
     *
     * @param the user to set alerts for
     * @param enabled Whether alerts should be enabled or disabled
     */
    boolean setAlertsEnabled(TGUser user, boolean enabled);
}
