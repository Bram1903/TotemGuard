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

package com.deathmotion.totemguard.common.event.channel.impl;

import com.deathmotion.totemguard.api.event.events.TGUserQuitEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;

public final class TGUserQuitChannel extends EventChannelImpl<TGUserQuitEvent> {

    public TGUserQuitChannel() {
        super(TGUserQuitEvent.class);
    }

    public void fire(@NotNull TGUser user) {
        if (isEmpty()) return;
        dispatch(new Holder(user));
    }

    private static final class Holder implements TGUserQuitEvent {
        private final TGUser user;

        Holder(TGUser user) {
            this.user = user;
        }

        @Override
        public @NotNull TGUser getUser() {
            return user;
        }
    }
}
