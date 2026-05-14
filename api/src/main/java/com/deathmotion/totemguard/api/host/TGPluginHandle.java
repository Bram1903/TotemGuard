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

package com.deathmotion.totemguard.api.host;

import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Loader-side handle to a running inner TotemGuard plugin. Returned by
 * {@link TGPluginEntry#start(TGPluginHost)}.
 */
public interface TGPluginHandle {

    /**
     * Synchronously stops the inner plugin. The implementation must fire
     * {@link TGPluginShutdownEvent} with the supplied {@code reason} before
     * tearing down internal state, unregister every platform-side hook
     * (PacketEvents listeners, platform listeners, Cloud commands, scheduled
     * tasks), and null out any static state that would keep the previous
     * classloader alive.
     * <p>
     * Safe to call once; subsequent calls are no-ops.
     */
    void stop(@NotNull TGPluginShutdownEvent.Reason reason);

    /**
     * Reports the running inner plugin's version. Used by the loader's
     * {@code /tgloader status} output.
     */
    @NotNull String version();
}
