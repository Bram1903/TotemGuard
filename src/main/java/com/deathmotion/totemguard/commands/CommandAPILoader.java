package com.deathmotion.totemguard.commands;

import com.deathmotion.totemguard.TotemGuard;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPILogger;

public class CommandAPILoader {

    private TotemGuard plugin;

    public CommandAPILoader(TotemGuard plugin) {
        this.plugin = plugin;
        init();
    }

    public void init() {
        CommandAPI.setLogger(CommandAPILogger.fromJavaLogger(plugin.getLogger()));
        CommandAPIBukkitConfig config = new CommandAPIBukkitConfig(plugin);
        config.usePluginNamespace();
        CommandAPI.onLoad(config);
    }

    public void enable() {
        CommandAPI.onEnable();
    }

    public void disable() {
        CommandAPI.onDisable();
    }
}
