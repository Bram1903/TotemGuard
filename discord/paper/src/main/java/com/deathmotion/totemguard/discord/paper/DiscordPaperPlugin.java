/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.discord.paper;

import com.deathmotion.totemguard.discord.DiscordService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordPaperPlugin extends JavaPlugin {
    private static final String RELOAD_PERMISSION = "totemguard.discord.reload";

    private DiscordService service;

    @Override
    public void onEnable() {
        this.service = new DiscordService(new PaperDiscordPlatform(this));
        this.service.start();
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.stop();
            service = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                sender.sendMessage("You do not have permission to reload the TotemGuard Discord bot.");
                return true;
            }
            if (service == null) {
                sender.sendMessage("The TotemGuard Discord bot is not running.");
                return true;
            }
            sender.sendMessage("Reloading the TotemGuard Discord bot. Check the console for the result.");
            service.reload();
            return true;
        }

        sender.sendMessage("Usage: /" + label + " reload");
        return true;
    }
}
