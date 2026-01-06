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
import com.deathmotion.totemguard.common.alert.AlertManagerImpl;
import com.deathmotion.totemguard.common.commands.Command;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class AlertCommand implements Command {

    private final AlertManagerImpl alertManager;

    public AlertCommand() {
        this.alertManager = TGPlatform.getInstance().getAlertManager();
    }

    @Override
    public void register(CommandManager<Sender> manager) {
        manager.command(
                manager.commandBuilder("totemguard", "tg")
                        .literal("alerts")
                        .permission("totemguard.alerts")
                        .handler(this::toggleAlerts)
        );
    }

    private void toggleAlerts(@NotNull CommandContext<Sender> context) {
        if (!context.sender().isPlayer()) {
            context.sender().sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED));
            return;
        }

        TGPlayer player = TGPlatform.getInstance().getPlayerRepository().getPlayer(context.sender().getUniqueId());
        if (player == null) {
            context.sender().sendMessage(Component.text("Your player data could not be found in the player repository", NamedTextColor.RED));
        }

        alertManager.toggleAlerts(player);
    }
}
