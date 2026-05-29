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

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.api.event.events.TGNetworkAlertEvent;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TGNetworkAlertChannel extends EventChannelImpl<TGNetworkAlertEvent> {

    public TGNetworkAlertChannel() {
        super(TGNetworkAlertEvent.class);
    }

    public void fire(@NotNull UUID playerUuid, @NotNull String playerName, @NotNull String checkName,
                     @NotNull CheckType checkType, int violations, @Nullable String debug,
                     @NotNull String serverName, @NotNull TGNetworkAlertEvent.Kind kind,
                     boolean remote, long timestamp) {
        if (isEmpty()) return;
        dispatch(new Holder(playerUuid, playerName, checkName, checkType, violations, debug,
                serverName, kind, remote, timestamp));
    }

    private record Holder(UUID playerUuid, String playerName, String checkName, CheckType checkType,
                          int violations, @Nullable String debug, String serverName,
                          TGNetworkAlertEvent.Kind kind, boolean remote, long timestamp)
            implements TGNetworkAlertEvent {

        @Override
        public @NotNull UUID getPlayerUuid() {
            return playerUuid;
        }

        @Override
        public @NotNull String getPlayerName() {
            return playerName;
        }

        @Override
        public @NotNull String getCheckName() {
            return checkName;
        }

        @Override
        public @NotNull CheckType getCheckType() {
            return checkType;
        }

        @Override
        public int getViolations() {
            return violations;
        }

        @Override
        public @Nullable String getDebug() {
            return debug;
        }

        @Override
        public @NotNull String getServerName() {
            return serverName;
        }

        @Override
        public @NotNull Kind getKind() {
            return kind;
        }

        @Override
        public boolean isRemote() {
            return remote;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
}
