package de.outdev.totemguard.commands;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.config.Settings;
import de.outdev.totemguard.data.PermissionConstants;
import de.outdev.totemguard.util.Util;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CheckCommand {

    private final TotemGuard plugin;
    private final Util util;
    private final Settings settings;

    public CheckCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.util = new Util(plugin);

        registerCheckCommand();
    }

    private void registerCheckCommand() {

        new CommandAPICommand("check")
                .withPermission("TotemGuard.Check")
                .withAliases("totemcheck", "checktotem")
                .withArguments(new PlayerArgument("target"))
                .executesPlayer((sender, args) -> {
                    Player player = (Player) args.get(0);

                    handleCheckCommand(sender, player);
                })
                .register();

    }

    private void handleCheckCommand(Player sender, Player player) {
        if (player == null) {
            Util.sendMiniMessage(sender, "<red>Player not found!");
            return;
        }

        if (!hasTotemInOffhand(sender)) {
            Util.sendMiniMessage(sender, "<red>This player has no totem in their offhand!");
            sender.playSound(sender, Sound.ENTITY_ENDERMAN_DEATH, 1, 1);
            return;
        }

        ItemStack totem = player.getInventory().getItemInOffHand();
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        if (settings.isToggleDamageOnCheck()) {
            double damage = settings.getDamageAmountOnCheck() > 0 ? settings.getDamageAmountOnCheck() : player.getHealth() / 1.25;
            damage = Math.min(damage, player.getHealth() - 1);
            if (damage > 0) {
                player.damage(damage);
            }
        }

        new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                if (hasTotemInOffhand(player)) {
                    failed(player, sender, elapsedTicks);
                    cancel();
                    player.getInventory().setItemInOffHand(totem); // Restore the totem
                } else if (elapsedTicks >= settings.getCheckTime()) {
                    Util.sendMiniMessage(sender, "<green>Concluded check. The player does not have a totem in their offhand. (" + settings.getCheckTime() + " ticks)");
                    player.getInventory().setItemInOffHand(totem); // Restore the totem
                    cancel();
                }
                elapsedTicks += 1;
            }
        }.runTaskTimer(plugin, 0L, 1);

    }


    private boolean hasTotemInOffhand(Player player) {
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        return itemInOffHand != null && itemInOffHand.getType() == Material.TOTEM_OF_UNDYING;
    }


    private void failed(Player player, Player executor, int elapsedTicks) { // TODO replace later
        String flag_01 = "§cS";
        String flag_02 = "§cB";
        String flag_03 = "§cM";
        if (player.isSneaking()) {
            flag_01 = "§aS";
        }
        if (player.isBlocking()) {
            flag_02 = "§aB";
        }
        if (player.isSprinting() || player.isClimbing() || player.isJumping()) {
            flag_03 = "§aM";
        }

        String extraFlags = "§8[§7" + flag_01 + "§7, " + flag_02 + "§7, " + flag_03 + "§8]";

        Util.sendMiniMessage(executor, settings.getCheckPrefix() + "&6" + player.getName() + " Failed the AutoTotem check &7(in: " + elapsedTicks + " ticks, Ping: " + player.getPing() + ", Brand: " + player.getClientBrandName() + ", Gamemode: " + player.getGameMode() + ", " + extraFlags + "&7) &8TPS: " + plugin.getTPS());
    }
}
