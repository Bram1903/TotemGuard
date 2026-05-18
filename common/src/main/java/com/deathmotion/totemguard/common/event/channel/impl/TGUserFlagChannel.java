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

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.event.events.TGUserFlagEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TGUserFlagChannel extends EventChannelImpl<TGUserFlagEvent> {

    public TGUserFlagChannel() {
        super(TGUserFlagEvent.class);
    }

    public boolean fire(@NotNull TGUser user, @NotNull Check check, @Nullable String debug) {
        if (isEmpty()) return false;
        Holder event = new Holder(user, check, debug);
        dispatch(event);
        return event.cancelled;
    }

    private static final class Holder implements TGUserFlagEvent {
        private final TGUser user;
        private final Check check;
        private final @Nullable String debug;
        boolean cancelled;

        Holder(TGUser user, Check check, @Nullable String debug) {
            this.user = user;
            this.check = check;
            this.debug = debug;
        }

        @Override
        public @NotNull TGUser getUser() {
            return user;
        }

        @Override
        public @NotNull Check getCheck() {
            return check;
        }

        @Override
        public @Nullable String getDebug() {
            return debug;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
