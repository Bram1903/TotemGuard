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

import com.deathmotion.totemguard.api.event.impl.TGMonitorOpenEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.api.EventImpl;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public final class TGMonitorOpenEventImpl extends EventImpl implements TGMonitorOpenEvent {

    private final @NotNull UUID viewerUuid;
    private final @NotNull UUID targetUuid;
    private final @NotNull String targetName;
    private final @Nullable TGUser targetUser;
    private final @NotNull UUID targetServerInstanceId;
    private final @NotNull String targetServerName;
    private final @Nullable String targetProxyServerId;
    private final boolean crossServer;
    private final boolean serverSwitch;

    @Setter
    private boolean cancelled;

    public TGMonitorOpenEventImpl(@NotNull UUID viewerUuid, @NotNull UUID targetUuid,
                                  @NotNull String targetName, @Nullable TGUser targetUser,
                                  @NotNull UUID targetServerInstanceId, @NotNull String targetServerName,
                                  @Nullable String targetProxyServerId,
                                  boolean crossServer, boolean serverSwitch) {
        this.viewerUuid = viewerUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetUser = targetUser;
        this.targetServerInstanceId = targetServerInstanceId;
        this.targetServerName = targetServerName;
        this.targetProxyServerId = targetProxyServerId;
        this.crossServer = crossServer;
        this.serverSwitch = serverSwitch;
    }
}
