package com.strealex.totemguard.commands;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.checks.Check;
import com.strealex.totemguard.config.Settings;
import com.strealex.totemguard.util.Util;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CheckCommand extends Check {

    private final TotemGuard plugin;
    private final Settings settings;

    public CheckCommand(TotemGuard plugin) {
        super(plugin, "AutoTotemManuelA", "Takes player's totem to bait their client into retoteming.", plugin.getConfigManager().getSettings().getPunish().getPunishAfter());

        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();

        registerCheckCommand();
    }

    private void registerCheckCommand() {
        new CommandAPICommand("check")
                .withPermission("TotemGuard.Check")
                .withAliases("totemcheck", "checktotem")
                .withArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player targetPlayer = (Player) args.get(0);
                    handleCheckCommand(sender, targetPlayer);
                })
                .register();
    }

    private void handleCheckCommand(CommandSender sender, Player player) {
        if (player == null) {
            Util.sendMiniMessage(sender, "<red>Player not found!");
            return;
        }

        if (!hasTotemInOffhand(player)) {
            Util.sendMiniMessage(sender, "<red>This player has no totem in their offhand!");
            if (sender instanceof Player) {
                ((Player) sender).playSound((Player) sender, Sound.ENTITY_ENDERMAN_DEATH, 1, 1);
            }
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
                    //flag(player, Component.text(sender.getName()));
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
}
