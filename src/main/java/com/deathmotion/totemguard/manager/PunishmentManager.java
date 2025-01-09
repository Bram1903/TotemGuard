/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.events.PunishEvent;
import com.deathmotion.totemguard.checks.Check;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PunishmentManager {
    private final TotemGuard plugin;

    private final Set<UUID> toBePunished = ConcurrentHashMap.newKeySet();

    public PunishmentManager(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public void punishPlayer(Check check, Component details) {
        if (!check.getCheckSettings().isPunishable()) return;
        if (check.getViolations() < check.getCheckSettings().getMaxViolations()) return;
        if (toBePunished.contains(check.getPlayer().getUniqueId())) return;

        if (check.getSettings().isApi()) {
            PunishEvent punishEvent = new PunishEvent(check.getPlayer(), check);
            plugin.getServer().getPluginManager().callEvent(punishEvent);
            if (punishEvent.isCancelled()) return;
        }

        toBePunished.add(check.getPlayer().getUniqueId());
        startPunishment(check, details);
    }

    private void startPunishment(Check check, Component details) {
        int delay = check.getCheckSettings().getPunishmentDelayInSeconds();
        if (delay <= 0) {
            executePunishment(check, details);
            toBePunished.remove(check.getPlayer().getUniqueId());
        } else {
            FoliaScheduler.getAsyncScheduler().runDelayed(plugin, (o) -> {
                executePunishment(check, details);
                toBePunished.remove(check.getPlayer().getUniqueId());
            }, delay, TimeUnit.SECONDS);
        }
    }

    private void executePunishment(Check check, Component details) {
        runPunishmentCommands(check);
        plugin.getDiscordManager().sendPunishment(check, details);
        plugin.getDatabaseService().savePunishment(check, details);
    }

    private void runPunishmentCommands(Check check) {
        String defaultPunishment = plugin.getConfigManager().getChecks().getDefaultPunishment();
        List<String> commands = check.getCheckSettings().getPunishmentCommands();
        List<String> processedCommands = new ArrayList<>();

        for (String command : commands) {
            String processedCommand = command
                    .replace("%default_punishment%", defaultPunishment)
                    .replace("%player%", check.getPlayer().user.getName())
                    .replace("%uuid%", check.getPlayer().user.getUUID().toString())
                    .replace("%check_name%", check.getCheckName())
                    .replace("%violations%", String.valueOf(check.getViolations()))
                    .replace("%max_violations%", String.valueOf(check.getCheckSettings().getMaxViolations()))
                    .replace("%server_name%", check.getSettings().getServer());

            processedCommands.add(processedCommand);
        }

        FoliaScheduler.getGlobalRegionScheduler().run(plugin, (o) -> {
            for (String command : processedCommands) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            }
        });
    }

}
