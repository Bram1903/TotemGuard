/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.CommandBuilder;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.Configurable;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class CloudCommandManager {

    private final @NotNull Logger log;
    private final @NotNull LegacyPaperCommandManager<CommandSender> commandManager;

    public CloudCommandManager(@NotNull TotemGuard plugin) {
        this.log = plugin.getLogger();
        this.commandManager = LegacyPaperCommandManager.createNative(plugin, ExecutionCoordinator.simpleCoordinator());

        log.info(() -> "Initializing command manager...");
        configureCapabilities();
        registerCommands();
    }

    private void configureCapabilities() {
        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            registerBrigadier();
        } else {
            log.info(() -> "Brigadier not available on this platform.");

            if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
                log.info(() -> "Asynchronous command completions enabled.");
            }
        }
    }


    private void registerBrigadier() {
        commandManager.registerBrigadier();

        final CloudBrigadierManager<CommandSender, ?> brigadier = commandManager.brigadierManager();
        final Configurable<BrigadierSetting> settings = brigadier.settings();

        settings.set(BrigadierSetting.FORCE_EXECUTABLE, true);

        log.info(() -> "Hooked into Brigadier for native command support.");
    }

    private void registerCommands() {
        new CommandBuilder(commandManager);
    }
}
