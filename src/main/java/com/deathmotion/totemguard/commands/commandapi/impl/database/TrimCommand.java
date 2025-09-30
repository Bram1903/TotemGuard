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

package com.deathmotion.totemguard.commands.commandapi.impl.database;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.commandapi.impl.database.util.ValidationHelper;
import com.deathmotion.totemguard.commands.commandapi.impl.database.util.ValidationType;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.messenger.impl.DatabaseMessageService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class TrimCommand {
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

    public CommandAPICommand init() {
        return new CommandAPICommand("trim")
                .withPermission("TotemGuard.Database.Trim")
                .withOptionalArguments(new IntegerArgument("code"))
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        Optional<Integer> optionalCode = args.getOptional("code").map(value -> (Integer) value);

        if (optionalCode.isEmpty()) {
            sender.sendMessage(validationHelper.generateCodeMessage(ValidationType.TRIM));
            return;
        }

        if (!validationHelper.validateCode(optionalCode.get())) {
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
