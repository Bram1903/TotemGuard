package de.outdev.totemguard;

import de.outdev.totemguardv2.commands.CheckCommand;
import de.outdev.totemguardv2.commands.TotemGuardCommand;
import de.outdev.totemguardv2.listeners.TotemUseListener;
import de.outdev.totemguardv2.config.ConfigManager;
import lombok.Getter;
import de.outdev.totemguard.commands.CheckCommand;
import de.outdev.totemguard.commands.TotemGuardCommand;
import de.outdev.totemguard.listeners.TotemUseListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class TotemGuard extends JavaPlugin {

    private static TotemGuard instance;
    private static FileConfiguration config;
    private TotemUseListener totemUseListener;
    @Getter
    private static TotemGuardV2 instance;

    @Nullable
    public static TotemGuard getInstance() {
        return instance;
    }

    @Nullable
    public static FileConfiguration getConfiguration() {
        return config;
    }
    public ConfigManager configManager;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        instance = this;
        configManager = new ConfigManager(this);

        if (!loadConfig()) {
            instance.getServer().getPluginManager().disablePlugin(instance);
            return;
        }

        registerCommands();
        registerListeners();

        getLogger().info(" \n"+
                "  _____    _             ___                  _ \n" +
                " |_   _|__| |_ ___ _ __ / __|_  _ __ _ _ _ __| |\n" +
                "   | |/ _ \\  _/ -_) '  \\ (_ | || / _` | '_/ _` |\n" +
                "   |_|\\___/\\__\\___|_|_|_\\___|\\_,_\\__,_|_| \\__,_|\n" +
                "                                                ");
        long finish = System.currentTimeMillis() - start;
        getLogger().info("Successfully loaded TotemGuard in " + finish + "ms!");
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
        new TotemUseListener(this);
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

    public double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getDeclaredMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return tps[0];
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}
