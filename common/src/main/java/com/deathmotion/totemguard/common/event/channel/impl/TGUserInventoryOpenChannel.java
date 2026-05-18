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

import com.deathmotion.totemguard.api.event.events.TGUserInventoryOpenEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;

public final class TGUserInventoryOpenChannel extends EventChannelImpl<TGUserInventoryOpenEvent> {

    public TGUserInventoryOpenChannel() {
        super(TGUserInventoryOpenEvent.class);
    }

    public void fire(@NotNull TGUser user, boolean serverInitiated) {
        if (isEmpty()) return;
        dispatch(new Holder(user, serverInitiated));
    }

    private static final class Holder implements TGUserInventoryOpenEvent {
        private final TGUser user;
        private final boolean serverInitiated;

        Holder(TGUser user, boolean serverInitiated) {
            this.user = user;
            this.serverInitiated = serverInitiated;
        }

        @Override
        public @NotNull TGUser getUser() {
            return user;
        }

        @Override
        public boolean isServerInitiated() {
            return serverInitiated;
        }
    }
}
