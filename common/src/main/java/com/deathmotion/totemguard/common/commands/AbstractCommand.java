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

package com.deathmotion.totemguard.common.commands;

import com.deathmotion.totemguard.api3.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCommand {

    protected abstract void register(@NotNull CommandManager<Sender> manager);

    protected Command.Builder<Sender> base(final @NotNull CommandManager<Sender> manager) {
        return CommandDefaults.root(manager);
    }

    protected String perm(final @NotNull String permission) {
        return CommandDefaults.PERMISSION_PREFIX + permission;
    }

    protected final boolean requirePlayer(final @NotNull Sender sender) {
        if (sender.isPlayer()) {
            return true;
        }
        sender.sendMessage(TGPlatform.getInstance().getMessageService().getComponent(MessagesKeys.GENERAL_PLAYER_ONLY));
        return false;
    }
}
