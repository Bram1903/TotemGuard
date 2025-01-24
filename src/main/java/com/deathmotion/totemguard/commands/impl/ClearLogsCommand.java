package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.CommandSuggestionUtil;
import com.deathmotion.totemguard.commands.OfflinePlayerCommandHandler;
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.impl.ClearLogsMessageService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

public class ClearLogsCommand {
    private final DatabaseService databaseService;
    private final CommandMessengerService commandMessengerService;
    private final ClearLogsMessageService clearLogsMessageService;

    public ClearLogsCommand(TotemGuard plugin) {
        this.databaseService = plugin.getDatabaseService();
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
        this.clearLogsMessageService = plugin.getMessengerService().getClearLogsMessageService();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("clearlogs")
                .withPermission("TotemGuard.ClearLogs")
                .withArguments(new AsyncOfflinePlayerArgument("target").replaceSuggestions(
                        CommandSuggestionUtil.getOfflinePlayerNameSuggestions()
                ))
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        CompletableFuture<OfflinePlayer> target = (CompletableFuture<OfflinePlayer>) args.get("target");
        sender.sendMessage(clearLogsMessageService.clearingStarted());

        OfflinePlayerCommandHandler.handlePlayerTarget(sender, target, this::handleCommand);
    }

    private void handleCommand(CommandSender sender, OfflinePlayer offlinePlayer) {
        long startTime = System.currentTimeMillis();
        int deletedRecords = databaseService.eraseLogs(offlinePlayer.getUniqueId());

        if (deletedRecords == -1) {
            sender.sendMessage(commandMessengerService.noDatabasePlayerFound(offlinePlayer.getName()));
            return;
        }

        long loadTime = System.currentTimeMillis() - startTime;
        sender.sendMessage(clearLogsMessageService.logsCleared(deletedRecords, offlinePlayer.getName(), loadTime));
    }
}
