package com.strealex.totemguard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.strealex.totemguard.checks.impl.badpackets.BadPacketsA;
import com.strealex.totemguard.checks.impl.totem.AutoTotemA;
import com.strealex.totemguard.checks.impl.totem.AutoTotemB;
import com.strealex.totemguard.commands.CheckCommand;
import com.strealex.totemguard.commands.TotemGuardCommand;
import com.strealex.totemguard.config.ConfigManager;
import com.strealex.totemguard.listeners.UserTracker;
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

        registerPacketListeners();
        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling plugin 'TotemGuard'...");
        saveDefaultConfig();
    }

    private void registerPacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(new UserTracker(this), PacketListenerPriority.LOW);
        PacketEvents.getAPI().getEventManager().registerListener(new BadPacketsA(this), PacketListenerPriority.NORMAL);
    }

    private void registerCommands() {
        new TotemGuardCommand(this);
        new CheckCommand(this);
    }

    private void registerListeners() {
        new AutoTotemA(this);
        new AutoTotemB(this);
        new UserTracker(this);
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
