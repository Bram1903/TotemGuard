package com.deathmotion.totemguard.commands.cloud.impl;

import com.deathmotion.totemguard.commands.cloud.BuildableCommand;
import com.deathmotion.totemguard.util.MessageUtil;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class ConsoleOnlyCommand implements BuildableCommand {
    @Override
    public void register(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("totemguard", "tg")
                        .literal("consoleonly", Description.of("A command that can only be run by the console"))
                        .permission("TotemGuard.ConsoleOnly")
                        .senderType(ConsoleCommandSender.class)
                        .handler(this::handleConsoleOnlyCommand)
        );
    }

    private void handleConsoleOnlyCommand(@NonNull CommandContext<ConsoleCommandSender> context) {
        ConsoleCommandSender console = context.sender();
        console.sendMessage(MessageUtil.getPrefix().append(Component.text(" Hello console!", NamedTextColor.GREEN)));
    }
}