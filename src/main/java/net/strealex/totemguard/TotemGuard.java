package net.strealex.totemguard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import net.strealex.totemguard.checks.impl.badpackets.BadPacketsA;
import net.strealex.totemguard.checks.impl.totem.AutoTotemA;
import net.strealex.totemguard.checks.impl.totem.AutoTotemB;
import net.strealex.totemguard.commands.CheckCommand;
import net.strealex.totemguard.commands.TotemGuardCommand;
import net.strealex.totemguard.config.ConfigManager;
import net.strealex.totemguard.listeners.UserTracker;
import net.strealex.totemguard.manager.AlertManager;
import lombok.Getter;
import net.strealex.totemguard.manager.DiscordManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class TotemGuard extends JavaPlugin {

    @Getter private static TotemGuard instance;
    @Getter private ConfigManager configManager;
    @Getter private UserTracker userTracker;
    @Getter private AlertManager alertManager;
    @Getter private DiscordManager discordManager;

    @Override
    public void onEnable() {

        instance = this;
        configManager = new ConfigManager(this);

        if (!loadConfig()) {
            instance.getServer().getPluginManager().disablePlugin(instance);
            return;
        }

        userTracker = new UserTracker(this);
        alertManager = new AlertManager(this);
        discordManager = new DiscordManager(this);

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
        PacketEvents.getAPI().getEventManager().registerListener(userTracker, PacketListenerPriority.LOW);
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

    public int getTps() {
        return (int) Math.round(Bukkit.getTPS()[0]);
    }
}
