package com.deathmotion.totemguard.commands.cloud;

import com.deathmotion.totemguard.TotemGuard;
import lombok.NonNull;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class TotemGuardCommand extends AbstractCommand {

    @Override
    public void register(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager).handler(this::handle));
    }

    private void handle(@NonNull CommandContext<CommandSender> context) {
        context.sender().sendMessage(TotemGuard.getInstance().getMessengerService().totemGuardInfo());
    }
}
