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

package com.deathmotion.totemguard.commands.impl.database;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.AbstractCommand;
import com.deathmotion.totemguard.commands.impl.database.util.ValidationHelper;
import com.deathmotion.totemguard.commands.impl.database.util.ValidationType;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.messenger.impl.DatabaseMessageService;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

public final class TrimCommand extends AbstractCommand {

    private final TotemGuard plugin;
    private final DatabaseProvider databaseProvider;
    private final DatabaseMessageService databaseMessageService;
    private final ValidationHelper validationHelper;

    public TrimCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseProvider = plugin.getDatabaseProvider();
        this.databaseMessageService = plugin.getMessengerService().getDatabaseMessageService();
        this.validationHelper = ValidationHelper.getInstance();
    }

    @Override
    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("database", Description.of("Database related commands"))
                .literal("trim", Description.of("Clears the entire database"))
                .optional("code", IntegerParser.integerParser())
                .permission(perm("Database.Trim"))
                .handler(this::handle)
        );
    }

    private void handle(@NonNull final CommandContext<CommandSender> ctx) {
        final CommandSender sender = ctx.sender();
        final Integer code = ctx.getOrDefault("code", null);

        if (code == null) {
            sender.sendMessage(validationHelper.generateCodeMessage(ValidationType.TRIM));
            return;
        }

        if (!validationHelper.validateCode(code)) {
            sender.sendMessage(databaseMessageService.invalidConfirmationCode());
            return;
        }

        sender.sendMessage(databaseMessageService.trimmingStartedComponent());
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            long startTime = System.currentTimeMillis();
            int totalRemovedLogs = databaseProvider.getGenericService().optimizeDatabase();
            long loadTime = System.currentTimeMillis() - startTime;

            sender.sendMessage(databaseMessageService.trimmingCompleted(totalRemovedLogs, loadTime));
        });
    }
}
