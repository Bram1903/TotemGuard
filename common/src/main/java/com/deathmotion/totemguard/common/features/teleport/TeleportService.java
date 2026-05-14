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

package com.deathmotion.totemguard.common.features.teleport;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.placeholder.holder.impl.TeleportCommandPlaceholder;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public final class TeleportService {

    public static final String SILENT_FLAG = "--silent";
    private static final String PLACEHOLDER = "%" + TeleportCommandPlaceholder.KEY + "%";
    private static final ThreadLocal<Boolean> SILENT_BYPASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final TGPlatform platform;

    public TeleportService(@NotNull TGPlatform platform) {
        this.platform = platform;
    }

    public static boolean isSilentDispatch() {
        return SILENT_BYPASS.get();
    }

    public void teleport(@NotNull Sender sender, @NotNull String targetName) {
        teleport(sender, targetName, false);
    }

    public boolean teleport(@NotNull UUID viewerUuid, @NotNull String targetName) {
        return teleport(viewerUuid, targetName, false);
    }

    public void teleport(@NotNull Sender sender, @NotNull String targetName, boolean silent) {
        String command = platform.getPlaceholderRepository().replace(
                PLACEHOLDER, null, null,
                Map.of("tg_player", targetName, "tg_silent", silent ? SILENT_FLAG : ""));
        if (command.startsWith("/")) command = command.substring(1);

        if (silent) {
            SILENT_BYPASS.set(Boolean.TRUE);
            try {
                sender.performCommand(command);
            } finally {
                SILENT_BYPASS.set(Boolean.FALSE);
            }
        } else {
            sender.performCommand(command);
        }
    }

    public boolean teleport(@NotNull UUID viewerUuid, @NotNull String targetName, boolean silent) {
        Sender sender = platform.createSender(viewerUuid);
        if (sender == null) return false;
        teleport(sender, targetName, silent);
        return true;
    }
}
