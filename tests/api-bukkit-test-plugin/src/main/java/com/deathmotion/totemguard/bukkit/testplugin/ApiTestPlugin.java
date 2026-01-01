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

package com.deathmotion.totemguard.bukkit.testplugin;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.event.impl.TGFlagEvent;
import com.deathmotion.totemguard.api.event.impl.TGUserJoinEvent;
import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.bukkit.testplugin.events.TGFlagEventListener;
import com.deathmotion.totemguard.bukkit.testplugin.events.TGUserJoinEventListener;
import com.deathmotion.totemguard.bukkit.testplugin.events.TGUserQuitEventListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ApiTestPlugin extends JavaPlugin {

    @Getter
    private static ApiTestPlugin instance;
    private final List<AutoCloseable> listeners = new ArrayList<>();

    public ApiTestPlugin() {
        instance = this;
    }

    public void onEnable() {
        TotemGuard.getAsync().thenAccept(api -> {
            getLogger().info("Hooked into TotemGuard version " + api.getVersion() + ".");

            listeners.add(api.getEventRepository().subscribe(TGUserJoinEvent.class, new TGUserJoinEventListener()));
            listeners.add(api.getEventRepository().subscribe(TGUserQuitEvent.class, new TGUserQuitEventListener()));
            listeners.add(api.getEventRepository().subscribe(TGFlagEvent.class, new TGFlagEventListener()));
        });
    }


    public void onDisable() {
        for (AutoCloseable l : listeners) {
            try {
                l.close();
            } catch (Exception ignored) {
            }
        }
        listeners.clear();
    }
}
