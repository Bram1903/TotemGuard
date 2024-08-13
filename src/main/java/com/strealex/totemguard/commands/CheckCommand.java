package com.strealex.totemguard.commands;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.checks.Check;
import com.strealex.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckCommand extends Check implements CommandExecutor, TabExecutor {

    private final TotemGuard plugin;

    public CheckCommand(TotemGuard plugin) {
        super(plugin, "ManualTotemA", "Attempts to bait the player into replacing their totem.");

        this.plugin = plugin;
        plugin.getCommand("check").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("TotemGuard.Check")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /check <player>", NamedTextColor.RED));
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return false;
        }

        if (target.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
            sender.sendMessage(Component.text("This player is not in survival mode!", NamedTextColor.RED));
            return false;
        }

        if (!hasTotemInOffhand(target)) {
            sender.sendMessage(Component.text("This player has no totem in their offhand!", NamedTextColor.RED));
            return false;
        }

        ItemStack originalTotem = removeTotemFromOffhand(target);
        final Settings.Checks.ManualTotemA settings = plugin.getConfigManager().getSettings().getChecks().getManualTotemA();
        applyDamageIfNeeded(target, settings);

        AtomicInteger elapsedMs = new AtomicInteger(0);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable task = () -> {
            if (hasTotemInOffhand(target)) {
                target.getInventory().setItemInOffHand(originalTotem);
                flag(target, createDetails(sender, elapsedMs.get()), settings);
                scheduler.shutdown();
            } else if (elapsedMs.getAndIncrement() >= settings.getCheckTime()) {
                target.getInventory().setItemInOffHand(originalTotem);
                sender.sendMessage(Component.text(target.getName() + " has passed the check successfully!", NamedTextColor.GREEN));
                scheduler.shutdown();
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("TotemGuard.Check") || args.length != 1) {
            return List.of();
        }

        if (sender instanceof Player) {
            String senderName = sender.getName().toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.toLowerCase().equals(senderName)) // Prevent self-suggestion
                    .toList();
        }

        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }

    private boolean hasTotemInOffhand(Player player) {
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        return itemInOffHand != null && itemInOffHand.getType() == Material.TOTEM_OF_UNDYING;
    }

    private ItemStack removeTotemFromOffhand(Player target) {
        ItemStack totem = target.getInventory().getItemInOffHand();
        target.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        return totem;
    }

    private void applyDamageIfNeeded(Player target, Settings.Checks.ManualTotemA settings) {
        if (settings.isToggleDamageOnCheck()) {
            double damage = calculateDamage(target, settings);
            if (damage > 0) {
                target.damage(damage);
            }
        }
    }

    private double calculateDamage(Player target, Settings.Checks.ManualTotemA settings) {
        double defaultDamage = target.getHealth() / 1.25;
        double damage = settings.getDamageAmountOnCheck() > 0 ? settings.getDamageAmountOnCheck() : defaultDamage;
        return Math.min(damage, target.getHealth() - 1);
    }

    private Component createDetails(CommandSender sender, int elapsedMs) {
        return Component.text()
                .append(Component.text("Staff: ", NamedTextColor.GRAY))
                .append(Component.text(sender.getName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Elapsed Ms: ", NamedTextColor.GRAY))
                .append(Component.text(elapsedMs, NamedTextColor.GOLD))
                .build();
    }
}
