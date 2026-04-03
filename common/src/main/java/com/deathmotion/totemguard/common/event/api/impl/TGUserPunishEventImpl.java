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

package com.deathmotion.totemguard.common.event.api.impl;

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.api3.event.impl.TGUserPunishEvent;
import com.deathmotion.totemguard.api3.user.TGUser;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
public final class TGUserPunishEventImpl extends TGUserEventImpl implements TGUserPunishEvent {

    private final Check check;
    private final @Nullable String debug;

    @Setter
    private boolean cancelled;

    public TGUserPunishEventImpl(TGUser user, Check check, @Nullable String debug) {
        super(user);
        this.check = check;
        this.debug = debug;
        this.cancelled = false;
    }
}
