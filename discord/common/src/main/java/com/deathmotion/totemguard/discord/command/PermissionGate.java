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

package com.deathmotion.totemguard.discord.command;

import com.deathmotion.totemguard.discord.config.BotConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PermissionGate {
    private final BotConfig.CommandsConfig config;

    public PermissionGate(@NotNull BotConfig.CommandsConfig config) {
        this.config = config;
    }

    private static boolean matches(@Nullable Member member, BotConfig.CommandPermission permission) {
        if (member == null) return false;
        if (permission.userIds().contains(member.getIdLong())) return true;
        for (Role role : member.getRoles()) {
            if (permission.roleIds().contains(role.getIdLong())) return true;
        }
        return false;
    }

    public boolean mayRun(@NotNull SlashCommand command, @NotNull SlashCommandContext context) {
        Member member = context.event().getMember();
        BotConfig.CommandPermission permission = config.permission(command.name(), command.control());

        if (command.control()) {
            if (member != null && member.hasPermission(Permission.ADMINISTRATOR)) return true;
            return !permission.isEmpty() && matches(member, permission);
        }

        return permission.isEmpty() || matches(member, permission);
    }
}
