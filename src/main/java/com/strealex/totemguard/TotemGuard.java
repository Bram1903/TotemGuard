package com.strealex.totemguard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.strealex.totemguard.checks.impl.Totem.AutoTotemA;
import com.strealex.totemguard.checks.impl.Totem.AutoTotemB;
import com.strealex.totemguard.checks.impl.BadPackets.BadPacketsA;
import com.strealex.totemguard.commands.CheckCommand;
import com.strealex.totemguard.commands.TotemGuardCommand;
import com.strealex.totemguard.config.ConfigManager;
import com.strealex.totemguard.listeners.PlayerJoin;
import com.strealex.totemguard.manager.AlertManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    @Getter
    private ConfigManager configManager;
    @Getter
    private AlertManager alertManager;

    @Override
    public void onEnable() {

        instance = this;
        configManager = new ConfigManager(this);

        if (!loadConfig()) {
            instance.getServer().getPluginManager().disablePlugin(instance);
            return;
        }

        alertManager = new AlertManager(this);

        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling plugin 'TotemGuard'...");
        saveDefaultConfig();
    }

    private void registerCommands() {
        new TotemGuardCommand(this);
        new CheckCommand(this);
    }

    private void registerListeners() {
        new AutoTotemA(this);
        new AutoTotemB(this);
        new PlayerJoin(this);
        PacketEvents.getAPI().getEventManager().registerListener(
                new BadPacketsA(this), PacketListenerPriority.NORMAL);

        PacketEvents.getAPI().init();
    }

    /**
     * Loads the plugin configuration.
     *
     * @return true if the configuration was loaded successfully, false otherwise.
     */
    private boolean loadConfig() {
        final Optional<Throwable> error = configManager.loadConfig();
        if (error.isPresent()) {
            instance.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load configuration", error.get());
            return false;
        }
        return true;
    }
}
