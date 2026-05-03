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

import com.deathmotion.totemguard.api.event.impl.TGFocusEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.api.EventImpl;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public final class TGFocusEventImpl extends EventImpl implements TGFocusEvent {

    private final @NotNull UUID callerUuid;
    private final @Nullable UUID targetUuid;
    private final @Nullable String targetName;
    private final @Nullable TGUser targetUser;
    private final @Nullable UUID targetServerInstanceId;
    private final @Nullable String targetServerName;
    private final @Nullable String targetProxyServerId;
    private final boolean enabling;
    private final boolean restore;

    @Setter
    private boolean cancelled;

    private TGFocusEventImpl(@NotNull UUID callerUuid, @Nullable UUID targetUuid,
                             @Nullable String targetName, @Nullable TGUser targetUser,
                             @Nullable UUID targetServerInstanceId, @Nullable String targetServerName,
                             @Nullable String targetProxyServerId, boolean enabling, boolean restore) {
        this.callerUuid = callerUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetUser = targetUser;
        this.targetServerInstanceId = targetServerInstanceId;
        this.targetServerName = targetServerName;
        this.targetProxyServerId = targetProxyServerId;
        this.enabling = enabling;
        this.restore = restore;
    }

    public static TGFocusEventImpl enabling(@NotNull UUID callerUuid, @NotNull UUID targetUuid,
                                            @NotNull String targetName, @Nullable TGUser targetUser,
                                            @NotNull UUID targetServerInstanceId,
                                            @NotNull String targetServerName,
                                            @Nullable String targetProxyServerId, boolean restore) {
        return new TGFocusEventImpl(callerUuid, targetUuid, targetName, targetUser,
                targetServerInstanceId, targetServerName, targetProxyServerId, true, restore);
    }

    public static TGFocusEventImpl disabling(@NotNull UUID callerUuid) {
        return new TGFocusEventImpl(callerUuid, null, null, null, null, null, null, false, false);
    }
}
