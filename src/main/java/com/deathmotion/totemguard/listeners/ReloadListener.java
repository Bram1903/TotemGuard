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

package com.deathmotion.totemguard.listeners;

import better.reload.api.ReloadEvent;
import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.ConfigManager;
import com.deathmotion.totemguard.util.MessageService;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ReloadListener implements Listener {

    private final TotemGuard plugin;
    private final ConfigManager configManager;
    private final MessageService messageService;

    public ReloadListener(TotemGuard plugin) {
        this.plugin = plugin;
        configManager = plugin.getConfigManager();
        messageService = plugin.getMessageService();
    }

    @EventHandler
    public void onReloadEvent(ReloadEvent event) {
        // Code directly copied from TotemGuardCommand#handleReloadCommand
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            configManager.reload();
            event.getCommandSender().sendMessage(messageService.getPluginReloaded());
        });
    }
}
