package de.outdev.totemguardv2.commands;

import de.outdev.totemguardv2.TotemGuardV2;
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

    public CheckCommand(TotemGuardV2 plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("check").setExecutor(this);
        this.plugin.getCommand("check").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You need to be a player to execute this command!");
            return false;
        }

        Player executor = (Player) sender;
        String checkPerm = plugin.getConfig().getString("check_permission");

        if (!executor.hasPermission(checkPerm)) {
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

        boolean toggleDamageOnCheck = plugin.getConfig().getBoolean("toggle_damage_on_check");
        double damageAmountOnCheck = plugin.getConfig().getDouble("damage_amount_on_check");
        int checkInterval = plugin.getConfig().getInt("check_interval");
        int checkTime = plugin.getConfig().getInt("check_time");

        // Apply damage immediately
        if (toggleDamageOnCheck) {
            double damage = damageAmountOnCheck > 0 ? damageAmountOnCheck : player.getHealth() / 2;
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
                } else if (elapsedTicks >= checkTime) {
                    sendMiniMessage(executor, "&aConcluded check. The player does not have a totem in their offhand. (" + checkTime + " ticks)");
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
        String prefix = plugin.getConfig().getString("check_prefix");

        sendMiniMessage(executor, prefix+"&6" + player.getName() + " Failed the AutoTotem check &7(in: " + elapsedTicks + " ticks, Ping: " + player.getPing() + ", Brand: " +player.getClientBrandName()+", Gamemode: " +player.getGameMode()+ ", " +extraFlags+"&7) &8TPS: " + plugin.getTPS());
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
