package net.strealex.totemguard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import net.strealex.totemguard.checks.impl.badpackets.BadPacketsA;
import net.strealex.totemguard.checks.impl.totem.AutoTotemA;
import net.strealex.totemguard.checks.impl.totem.AutoTotemB;
import net.strealex.totemguard.commands.CheckCommand;
import net.strealex.totemguard.commands.TotemGuardCommand;
import net.strealex.totemguard.config.ConfigManager;
import net.strealex.totemguard.listeners.UserTracker;
import net.strealex.totemguard.manager.AlertManager;
import net.strealex.totemguard.manager.DiscordManager;
import net.strealex.totemguard.manager.PunishmentManager;
import net.strealex.totemguard.util.TGVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    @Getter
    private TGVersion version;

    @Getter
    private ConfigManager configManager;
    @Getter
    private AlertManager alertManager;
    @Getter
    private UserTracker userTracker;
    @Getter
    private DiscordManager discordManager;
    @Getter
    private PunishmentManager punishmentManager;

    @Override
    public void onEnable() {
        instance = this;
        version = TGVersion.createFromPackageVersion();

        configManager = new ConfigManager(this);

        if (!loadConfig()) {
            instance.getServer().getPluginManager().disablePlugin(instance);
            return;
        }

        alertManager = new AlertManager(this);
        userTracker = new UserTracker(this);
        discordManager = new DiscordManager(this);
        punishmentManager = new PunishmentManager(this);

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
