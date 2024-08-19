package net.strealex.totemguard.manager;

import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.data.CheckDetails;
import net.strealex.totemguard.data.TotemPlayer;

public class PunishmentManager {
    private final TotemGuard plugin;
    private final DiscordManager discordManager;

    public PunishmentManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.discordManager = plugin.getDiscordManager();
    }

    public boolean handlePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        if (!checkDetails.isPunishable()) return false;

        if (checkDetails.getViolations() >= checkDetails.getMaxViolations()) {
            String[] punishmentCommands = checkDetails.getPunishmentCommands();
            for (String command : punishmentCommands) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("%player%", totemPlayer.getUsername()));
            }

            discordManager.sendPunishment(totemPlayer, checkDetails);
            return true;
        }

        return false;
    }
}
