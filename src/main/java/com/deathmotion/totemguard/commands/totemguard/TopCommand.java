/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.commands.totemguard;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.models.TotemPlayer;
import com.deathmotion.totemguard.commands.SubCommand;
import com.deathmotion.totemguard.manager.CheckManager;
import com.deathmotion.totemguard.models.TopViolation;
import com.deathmotion.totemguard.models.checks.CheckRecord;
import com.deathmotion.totemguard.packetlisteners.UserTracker;
import com.deathmotion.totemguard.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class TopCommand implements SubCommand {
    private final CheckManager checkManager;
    private final UserTracker userTracker;
    private final MessageService messageService;

    public TopCommand(TotemGuard plugin) {
        this.checkManager = plugin.getCheckManager();
        this.userTracker = plugin.getUserTracker();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<CheckRecord> violations = checkManager.getViolations();

        if (violations.isEmpty()) {
            sender.sendMessage(noViolationsFound());
            return false;
        }

        Map<UUID, TopViolation> topViolationsMap = new HashMap<>();

        for (CheckRecord checkRecord : violations) {
            String checkName = checkRecord.checkName();
            Map<UUID, Integer> recordViolations = checkRecord.violations();

            for (Map.Entry<UUID, Integer> entry : recordViolations.entrySet()) {
                UUID uuid = entry.getKey();
                int violationCount = entry.getValue();

                String username = userTracker.getTotemPlayer(uuid)
                        .map(TotemPlayer::username)
                        .orElse("Unknown Player (" + uuid + ")");

                // Update or create a TopViolation for this user
                topViolationsMap.compute(uuid, (key, existing) -> {
                    if (existing == null) {
                        Map<String, Integer> checkViolations = new HashMap<>();
                        checkViolations.put(checkName, violationCount);
                        return new TopViolation(username, violationCount, checkViolations);
                    } else {
                        existing.checkViolations().put(checkName, violationCount);
                        return new TopViolation(
                                existing.username(),
                                existing.violations() + violationCount,
                                existing.checkViolations()
                        );
                    }
                });
            }
        }

        List<TopViolation> topViolations = new ArrayList<>(topViolationsMap.values());
        sender.sendMessage(messageService.getTopComponent(topViolations));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    private Component noViolationsFound() {
        return messageService.getPrefix().append(
                Component.text("No violations found.", NamedTextColor.RED)
        );
    }
}
