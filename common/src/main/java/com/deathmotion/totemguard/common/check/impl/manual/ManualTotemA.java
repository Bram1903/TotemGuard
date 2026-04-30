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

package com.deathmotion.totemguard.common.check.impl.manual;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.ManualCheck;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;

@CheckData(description = "Staff-forced totem check", type = CheckType.AUTO_TOTEM)
public final class ManualTotemA extends CheckImpl implements ManualCheck {

    public ManualTotemA(TGPlayer player) {
        super(player);
    }

    /**
     * Called by {@code /tg check} when the target re-totemed within the window.
     * Routes through the normal {@link CheckImpl#fail(String)} path so alerts
     * hit the chat buffer, Redis sync, and punishment repository like any
     * other flag.
     */
    public boolean handle(Sender staff, long elapsedMs, long windowMs) {
        return fail(debugInfo(staff, elapsedMs, windowMs));
    }

    private String debugInfo(Sender staff, long elapsedMs, long windowMs) {
        return "Staff=" + staff.getName()
                + ", Elapsed=" + elapsedMs + "ms"
                + ", Window=" + windowMs + "ms";
    }
}
