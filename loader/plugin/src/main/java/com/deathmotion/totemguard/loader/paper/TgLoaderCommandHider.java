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

package com.deathmotion.totemguard.loader.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Collection;
import java.util.Iterator;

public final class TgLoaderCommandHider implements Listener {

    private static final String PERMISSION = "totemguard.loader.admin";

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission(PERMISSION)) return;

        Collection<String> commands = event.getCommands();
        Iterator<String> it = commands.iterator();
        while (it.hasNext()) {
            String name = it.next();
            int colon = name.indexOf(':');
            String bare = colon >= 0 ? name.substring(colon + 1) : name;
            if ("tgloader".equalsIgnoreCase(bare)) {
                it.remove();
            }
        }
    }
}
