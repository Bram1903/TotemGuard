/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.bukkit.testplugin.events;

import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.bukkit.testplugin.ApiTestPlugin;

import java.util.function.Consumer;

public final class ACUserQuitEventListener implements Consumer<TGUserQuitEvent> {

    @Override
    public void accept(TGUserQuitEvent event) {
        ApiTestPlugin.getInstance().getLogger().info("User " + event.getUser().getName() + " quit the server.");
    }

}


