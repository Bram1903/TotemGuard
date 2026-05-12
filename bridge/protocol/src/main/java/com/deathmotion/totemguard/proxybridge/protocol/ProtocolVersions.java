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

package com.deathmotion.totemguard.proxybridge.protocol;

import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProtocolVersions {

    public static final String CURRENT = BridgeProtocol.VERSION;

    private ProtocolVersions() {
    }

    public static @Nullable String peekVersion(@NotNull String message) {
        int sep = message.indexOf(BridgeProtocol.FIELD);
        if (sep <= 0) return null;
        return message.substring(0, sep);
    }
}
