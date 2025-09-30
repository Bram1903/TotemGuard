package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.cloud.CommandBuilder;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class CloudCommandManager {
    private final LegacyPaperCommandManager<CommandSender> commandManager;

    public CloudCommandManager(TotemGuard plugin) {
        commandManager = LegacyPaperCommandManager.createNative(
                plugin,
                ExecutionCoordinator.simpleCoordinator()
        );

        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
            plugin.getLogger().info("Hooked into Brigadier for native command support");
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
            plugin.getLogger().info("Asynchronous command completions support enabled");
        }

        new CommandBuilder(commandManager);
    }
}
