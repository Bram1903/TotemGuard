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

import com.deathmotion.totemguard.api.event.events.TGFocusEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TGFocusChannel extends EventChannelImpl<TGFocusEvent> {

    public TGFocusChannel() {
        super(TGFocusEvent.class);
    }

    public boolean fireEnabling(@NotNull UUID callerUuid, @NotNull UUID targetUuid,
                                @NotNull String targetName, @Nullable TGUser targetUser,
                                @NotNull UUID targetServerInstanceId, @NotNull String targetServerName,
                                boolean restore) {
        return fire0(callerUuid, targetUuid, targetName, targetUser,
                targetServerInstanceId, targetServerName, true, restore);
    }

    public boolean fireDisabling(@NotNull UUID callerUuid) {
        return fire0(callerUuid, null, null, null, null, null, false, false);
    }

    private boolean fire0(@NotNull UUID callerUuid, @Nullable UUID targetUuid,
                          @Nullable String targetName, @Nullable TGUser targetUser,
                          @Nullable UUID targetServerInstanceId, @Nullable String targetServerName,
                          boolean enabling, boolean restore) {
        if (isEmpty()) return false;
        Holder event = new Holder(callerUuid, targetUuid, targetName, targetUser,
                targetServerInstanceId, targetServerName, enabling, restore);
        dispatch(event);
        return event.cancelled;
    }

    private static final class Holder implements TGFocusEvent {
        private final UUID callerUuid;
        private final @Nullable UUID targetUuid;
        private final @Nullable String targetName;
        private final @Nullable TGUser targetUser;
        private final @Nullable UUID targetServerInstanceId;
        private final @Nullable String targetServerName;
        private final boolean enabling;
        private final boolean restore;
        boolean cancelled;

        Holder(UUID callerUuid, @Nullable UUID targetUuid, @Nullable String targetName,
               @Nullable TGUser targetUser, @Nullable UUID targetServerInstanceId,
               @Nullable String targetServerName, boolean enabling, boolean restore) {
            this.callerUuid = callerUuid;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.targetUser = targetUser;
            this.targetServerInstanceId = targetServerInstanceId;
            this.targetServerName = targetServerName;
            this.enabling = enabling;
            this.restore = restore;
        }

        @Override
        public @NotNull UUID getCallerUuid() {
            return callerUuid;
        }

        @Override
        public @Nullable UUID getTargetUuid() {
            return targetUuid;
        }

        @Override
        public @Nullable String getTargetName() {
            return targetName;
        }

        @Override
        public @Nullable TGUser getTargetUser() {
            return targetUser;
        }

        @Override
        public @Nullable UUID getTargetServerInstanceId() {
            return targetServerInstanceId;
        }

        @Override
        public @Nullable String getTargetServerName() {
            return targetServerName;
        }

        @Override
        public boolean isEnabling() {
            return enabling;
        }

        @Override
        public boolean isRestore() {
            return restore;
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
