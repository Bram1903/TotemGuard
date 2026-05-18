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

import com.deathmotion.totemguard.api.event.events.TGMonitorOpenEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TGMonitorOpenChannel extends EventChannelImpl<TGMonitorOpenEvent> {

    public TGMonitorOpenChannel() {
        super(TGMonitorOpenEvent.class);
    }

    public boolean fire(@NotNull UUID viewerUuid, @NotNull UUID targetUuid,
                        @NotNull String targetName, @Nullable TGUser targetUser,
                        @NotNull UUID targetServerInstanceId, @NotNull String targetServerName,
                        boolean crossServer, boolean serverSwitch) {
        if (isEmpty()) return false;
        Holder event = new Holder(viewerUuid, targetUuid, targetName, targetUser,
                targetServerInstanceId, targetServerName, crossServer, serverSwitch);
        dispatch(event);
        return event.cancelled;
    }

    private static final class Holder implements TGMonitorOpenEvent {
        private final UUID viewerUuid;
        private final UUID targetUuid;
        private final String targetName;
        private final @Nullable TGUser targetUser;
        private final UUID targetServerInstanceId;
        private final String targetServerName;
        private final boolean crossServer;
        private final boolean serverSwitch;
        boolean cancelled;

        Holder(UUID viewerUuid, UUID targetUuid, String targetName, @Nullable TGUser targetUser,
               UUID targetServerInstanceId, String targetServerName,
               boolean crossServer, boolean serverSwitch) {
            this.viewerUuid = viewerUuid;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.targetUser = targetUser;
            this.targetServerInstanceId = targetServerInstanceId;
            this.targetServerName = targetServerName;
            this.crossServer = crossServer;
            this.serverSwitch = serverSwitch;
        }

        @Override
        public @NotNull UUID getViewerUuid() {
            return viewerUuid;
        }

        @Override
        public @NotNull UUID getTargetUuid() {
            return targetUuid;
        }

        @Override
        public @NotNull String getTargetName() {
            return targetName;
        }

        @Override
        public @Nullable TGUser getTargetUser() {
            return targetUser;
        }

        @Override
        public @NotNull UUID getTargetServerInstanceId() {
            return targetServerInstanceId;
        }

        @Override
        public @NotNull String getTargetServerName() {
            return targetServerName;
        }

        @Override
        public boolean isCrossServer() {
            return crossServer;
        }

        @Override
        public boolean isServerSwitch() {
            return serverSwitch;
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
