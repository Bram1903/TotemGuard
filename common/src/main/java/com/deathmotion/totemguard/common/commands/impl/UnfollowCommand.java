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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.network.follow.FollowRepository;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public final class UnfollowCommand extends AbstractCommand {

    private final TGPlatform platform;

    public UnfollowCommand() {
        this.platform = TGPlatform.getInstance();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("unfollow")
                        .permission(perm("follow"))
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) return;

        FollowRepository follow = platform.getFollowRepository();
        if (follow == null) return;

        if (follow.endFollow(sender.getUniqueId())) {
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_DISABLED));
        } else {
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_NONE_ACTIVE));
        }
    }
}
