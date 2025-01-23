package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.CommandSuggestionUtil;
import com.deathmotion.totemguard.commands.OfflinePlayerCommandHandler;
import com.deathmotion.totemguard.database.DatabaseService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

public class ClearLogsCommand {
    private final TotemGuard plugin;
    private final DatabaseService databaseService;

    public ClearLogsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseService = plugin.getDatabaseService();
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
        String targetRawName = args.getRaw("target");
        // TODO: Send an early message indicating that the command is being processed

        OfflinePlayerCommandHandler.handlePlayerTarget(sender, target, this::handleCommand);
    }

    private void handleCommand(CommandSender sender, OfflinePlayer offlinePlayer) {
        sender.sendMessage("Logs cleared for " + offlinePlayer.getName());
    }
}
