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
import com.deathmotion.totemguard.common.features.update.fleet.FleetUpdateService;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

/**
 * Fleet-wide loader update. Each instance re-resolves its own loader config and acts.
 */
public final class UpdateCommand extends AbstractCommand {

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("update")
                        .permission(perm("update"))
                        .flag(manager.flagBuilder("check").withAliases("c"))
                        .flag(manager.flagBuilder("force").withAliases("f"))
                        .flag(manager.flagBuilder("restart").withAliases("r"))
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        TGPlatform platform = TGPlatform.getInstance();
        FleetUpdateService service = platform.getFleetUpdateService();
        if (service == null) {
            context.sender().sendMessage(Component.text(
                    "Fleet update service is unavailable.", NamedTextColor.RED));
            return;
        }

        boolean dryRun = context.flags().contains("check");
        boolean force = context.flags().contains("force");
        boolean restart = context.flags().contains("restart");

        if (dryRun && restart) {
            context.sender().sendMessage(Component.text(
                    "--check and --restart are mutually exclusive.", NamedTextColor.RED));
            return;
        }

        service.issueUpdate(context.sender(), force, dryRun, restart);
    }
}
