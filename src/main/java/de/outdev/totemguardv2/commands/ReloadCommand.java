package de.outdev.totemguardv2.commands;

import de.outdev.totemguardv2.TotemGuardV2;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private TotemGuardV2 plugin = TotemGuardV2 .getInstance();

    public ReloadCommand(TotemGuardV2  plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("totemguard").setExecutor(this);
        this.plugin.getCommand("totemguard").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cPlease provide an argument!");
            return false;
        }

        String commandPerm = plugin.getConfig().getString("command_permissions");

        if (!(sender.hasPermission(commandPerm))) {
            sender.sendMessage("§cYou do not have permission to use this!");
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§aReloaded configuration!");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("reload");
    }
}
