package de.outdev.totemguardv2.commands;

import de.outdev.totemguardv2.TotemGuardV2;
import de.outdev.totemguardv2.data.PermissionConstants;
import de.outdev.totemguardv2.data.Settings;
import de.outdev.totemguardv2.manager.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TotemGuardCommand implements CommandExecutor, TabCompleter {

    private final TotemGuardV2 plugin;
    private final ConfigManager configManager;

    private static final Map<UUID, Boolean> alertToggle = new HashMap<>();

    public TotemGuardCommand(TotemGuardV2 plugin) {
        this.plugin = plugin;
        this.configManager = plugin.configManager;

        this.plugin.getCommand("totemguard").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cPlease provide an argument!");
            return false;
        }

        String prefix = configManager.getSettings().getPrefix().replace("&", "§");;

        if (!(sender.hasPermission(PermissionConstants.CommandPermission))) {
            sender.sendMessage("§cYou do not have permission to use this!");
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            configManager.reload();
            sender.sendMessage(prefix+"§aReloaded configuration!");
        }

        if (args[0].equalsIgnoreCase("alerts")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can toggle alerts!");
                return false;
            }

            boolean currentState = getToggle(player);
            setToggle(player, !currentState);
            sender.sendMessage(prefix+"§aAlerts " + (currentState ? "§cdisabled" : "§aenabled") + "§a!");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("reload", "alerts");
    }

    public static boolean getToggle(Player player) {
        return alertToggle.getOrDefault(player.getUniqueId(), true);
    }

    public static void setToggle(Player player, boolean value) {
        alertToggle.put(player.getUniqueId(), value);
    }
}
