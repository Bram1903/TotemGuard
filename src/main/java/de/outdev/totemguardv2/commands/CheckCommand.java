package de.outdev.totemguardv2.commands;

import de.outdev.totemguardv2.TotemGuardV2;
import de.outdev.totemguardv2.data.PermissionConstants;
import de.outdev.totemguardv2.data.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CheckCommand implements CommandExecutor, TabCompleter {

    private final TotemGuardV2 plugin;
    private final Settings settings;

    public CheckCommand(TotemGuardV2 plugin) {
        this.plugin = plugin;
        this.settings = plugin.configManager.getSettings();

        this.plugin.getCommand("check").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player executor)) {
            sender.sendMessage(ChatColor.RED + "You need to be a player to execute this command!");
            return false;
        }

        if (!executor.hasPermission(PermissionConstants.CheckPermission)) {
            sendMiniMessage(executor, "&cYou do not have the required permissions to execute this command!");
            return false;
        }

        if (args.length != 1) {
            sendMiniMessage(executor, "&cUsage: /check <player>");
            return false;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sendMiniMessage(executor, "&cPlayer not found!");
            return false;
        }

        if (!hasTotemInOffhand(player)) {
            sendMiniMessage(executor, "&cThis player has no totem in their offhand!");
            executor.playSound(executor.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1, 1);
            return true;
        }

        ItemStack totem = player.getInventory().getItemInOffHand();
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        // Apply damage immediately
        if (settings.isToggleDamageOnCheck()) {
            double damage = settings.getDamageAmountOnCheck() > 0 ? settings.getDamageAmountOnCheck() : player.getHealth() / 1.25;
            damage = Math.min(damage, player.getHealth() - 1);
            if (damage > 0) {
                player.damage(damage);
            }
        }

        // Schedule a task to run periodically and check for the totem
        new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                if (hasTotemInOffhand(player)) {
                    failed(player, executor, elapsedTicks);
                    cancel();
                    player.getInventory().setItemInOffHand(totem); // Restore the totem
                } else if (elapsedTicks >= settings.getCheckTime()) {
                    sendMiniMessage(executor, "&aConcluded check. The player does not have a totem in their offhand. (" + settings.getCheckTime() + " ticks)");
                    player.getInventory().setItemInOffHand(totem); // Restore the totem
                    cancel();
                }
                elapsedTicks += 1;
            }
        }.runTaskTimer(plugin, 0L, 1); // Check every checkInterval ticks

        return true;
    }

    private boolean hasTotemInOffhand(Player player) {
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        return itemInOffHand != null && itemInOffHand.getType() == Material.TOTEM_OF_UNDYING;
    }

    private void sendMiniMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void failed(Player player, Player executor, int elapsedTicks) {
        String flag_01 = "§cS";
        String flag_02 = "§cB";
        String flag_03 = "§cM";
        if (player.isSneaking()) {
            flag_01 = "§aS";
        }
        if (player.isBlocking()) {
            flag_02 = "§aB";
        }
        if (player.isSprinting() || player.isClimbing() || player.isJumping()) { // Set in the config later
            flag_03 = "§aM";
        }

        String extraFlags = "§8[§7" + flag_01 + "§7, " + flag_02 +"§7, " + flag_03+"§8]";

        sendMiniMessage(executor, settings.getCheckPrefix() + "&6" + player.getName() + " Failed the AutoTotem check &7(in: " + elapsedTicks + " ticks, Ping: " + player.getPing() + ", Brand: " +player.getClientBrandName()+", Gamemode: " +player.getGameMode()+ ", " +extraFlags+"&7) &8TPS: " + plugin.getTPS());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }
        return null;
    }
}
