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
import com.deathmotion.totemguard.common.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.alert.TesterAlertRoster;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public final class TesterCommand extends AbstractCommand {

    private final TGPlatform platform;
    private final AlertRepositoryImpl alertRepository;

    public TesterCommand() {
        this.platform = TGPlatform.getInstance();
        this.alertRepository = platform.getAlertRepository();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("tester")
                        .permission(perm("Tester"))
                        .handler(this::toggleTester)
        );
    }

    private void toggleTester(final @NotNull CommandContext<Sender> context) {
        final Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        UUID uuid = sender.getUniqueId();
        TesterAlertRoster roster = alertRepository.getTesterAlertRoster();

        if (roster.contains(uuid)) {
            roster.disable(uuid);
            return;
        }

        PlatformUserCreation creation = platform.getPlatformUserFactory().create(uuid);
        if (creation == null) return;

        roster.enable(uuid, creation.getPlatformUser());
    }
}
