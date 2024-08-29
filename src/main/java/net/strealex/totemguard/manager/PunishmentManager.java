package net.strealex.totemguard.manager;

import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.data.CheckDetails;
import net.strealex.totemguard.data.TotemPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Blocking;

import java.util.concurrent.CountDownLatch;

public class PunishmentManager {
    private final TotemGuard plugin;
    private final DiscordManager discordManager;

    public PunishmentManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.discordManager = plugin.getDiscordManager();
    }

    @Blocking
    public boolean handlePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        if (checkDetails.isPunishable() && checkDetails.getViolations() == checkDetails.getMaxViolations()) {
            long delay = checkDetails.getPunishmentDelay() * 20L;
            if (delay <= 0) {
                executePunishment(totemPlayer, checkDetails);
            } else {
                return scheduleAndWaitPunishment(totemPlayer, checkDetails, delay);
            }
            return true;
        }
        return false;
    }

    private void executePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            runPunishmentCommands(totemPlayer, checkDetails);
            discordManager.sendPunishment(totemPlayer, checkDetails);
        });
    }

    private boolean scheduleAndWaitPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails, long delay) {
        CountDownLatch latch = new CountDownLatch(1);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            runPunishmentCommands(totemPlayer, checkDetails);
            discordManager.sendPunishment(totemPlayer, checkDetails);
            latch.countDown(); // Signal that the task is complete
        }, delay);

        try {
            // Wait for the task to complete
            latch.await(); // This can block until latch.countDown() is called
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Handle interrupt
            return false; // Return false if interrupted
        }

        return true; // Only return true once punishment has been executed
    }

    private void runPunishmentCommands(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        String[] punishmentCommands = checkDetails.getPunishmentCommands();
        for (String command : punishmentCommands) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("%player%", totemPlayer.getUsername()));
        }
    }
}