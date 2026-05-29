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

import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TGDiagnosticChannel extends EventChannelImpl<TGDiagnosticEvent> {

    public TGDiagnosticChannel() {
        super(TGDiagnosticEvent.class);
    }

    public void fire(@NotNull TGDiagnosticEvent.Severity severity, @NotNull String subsystem,
                     @NotNull String message, @Nullable String stackTrace, @NotNull String serverName,
                     boolean remote, long timestamp) {
        if (isEmpty()) return;
        dispatch(new Holder(severity, subsystem, message, stackTrace, serverName, remote, timestamp));
    }

    private record Holder(TGDiagnosticEvent.Severity severity, String subsystem, String message,
                          @Nullable String stackTrace, String serverName, boolean remote, long timestamp)
            implements TGDiagnosticEvent {

        @Override
        public @NotNull Severity getSeverity() {
            return severity;
        }

        @Override
        public @NotNull String getSubsystem() {
            return subsystem;
        }

        @Override
        public @NotNull String getMessage() {
            return message;
        }

        @Override
        public @Nullable String getStackTrace() {
            return stackTrace;
        }

        @Override
        public @NotNull String getServerName() {
            return serverName;
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
