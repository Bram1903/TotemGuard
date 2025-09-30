package com.deathmotion.totemguard.commands.cloud.impl;

import com.deathmotion.totemguard.commands.cloud.BuildableCommand;
import com.deathmotion.totemguard.util.MessageUtil;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class PlayerOnlyCommand implements BuildableCommand {
    @Override
    public void register(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("totemguard", "tg")
                        .literal("playeronly", Description.of("A command that can only be run by a player"))
                        .permission("TotemGuard.PlayerOnly")
                        .senderType(Player.class)
                        .handler(this::handlePlayerOnlyCommand)
        );
    }

    private void handlePlayerOnlyCommand(@NonNull CommandContext<Player> context) {
        Player player = context.sender();
        player.sendMessage(MessageUtil.getPrefix().append(Component.text(" Hello " + player.getName() + "!", NamedTextColor.GREEN)));
    }
}
