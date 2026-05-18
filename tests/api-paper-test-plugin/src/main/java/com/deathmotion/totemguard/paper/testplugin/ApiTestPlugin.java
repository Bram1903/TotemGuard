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

package com.deathmotion.totemguard.paper.testplugin;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.event.EventBus;
import com.deathmotion.totemguard.api.event.EventPriority;
import com.deathmotion.totemguard.api.event.events.*;
import com.deathmotion.totemguard.api.user.InventoryStatus;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.paper.testplugin.listeners.InventoryActivityListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ApiTestPlugin extends JavaPlugin {

    private final InventoryActivityListener inventoryListener = new InventoryActivityListener(getLogger());
    private volatile boolean disabled;

    @Override
    public void onEnable() {
        hook();
    }

    @Override
    public void onDisable() {
        disabled = true;
        TotemGuardAPI api = TotemGuard.get();
        if (api != null) api.getEventBus().unregisterAll(this);
    }

    private void hook() {
        TotemGuard.getAsync().thenAccept(api -> {
            if (disabled) return;
            getLogger().info("Hooked into TotemGuard " + api.getVersion());
            subscribe(api.getEventBus());
        });
    }

    private void subscribe(EventBus bus) {
        bus.get(TGUserJoinEvent.class).subscribe(this, event -> {
            TGUser user = event.getUser();
            InventoryStatus inv = user.getInventoryStatus();
            getLogger().info(user.getName() + " joined (inventory "
                    + (inv.open() ? "open" : "closed") + ")");
        });

        bus.get(TGUserInventoryOpenEvent.class).subscribe(this, inventoryListener::onOpen);
        bus.get(TGUserInventoryCloseEvent.class).subscribe(this, inventoryListener::onClose);
        bus.get(TGUserQuitEvent.class).subscribe(this, inventoryListener::onQuit);

        bus.get(TGModDetectionResolvedEvent.class).subscribe(this, event ->
                getLogger().warning("[mods] " + event.getUser().getName() + " -> "
                        + event.getAction() + " with " + event.getDetectedMods().size() + " mod(s)"
                        + (event.isLate() ? " (late)" : "")));

        bus.get(TGUserPunishEvent.class).subscribe(this, event ->
                getLogger().warning("[punish] " + event.getUser().getName()
                        + " punished for " + event.getCheck().getName()));

        bus.get(TGTeleportEvent.class).subscribe(this, event ->
                getLogger().info("[teleport] " + event.getCallerUuid() + " -> "
                        + event.getTargetName() + (event.isCrossServer() ? " (cross-server)" : "")));

        bus.get(TGCheckEvent.class).subscribe(this, event ->
                        getLogger().info("[check] " + event.getName() + " on " + event.getUser().getName()
                                + (event.isCancelled() ? " (cancelled)" : "")),
                EventPriority.MONITOR, true);

        bus.subscribe(TGPluginShutdownEvent.class, this, this::onTotemGuardShutdown);
    }

    private void onTotemGuardShutdown(TGPluginShutdownEvent event) {
        TGPluginShutdownEvent.Reason reason = event.getReason();
        getLogger().info("TotemGuard " + event.getVersion() + " shutting down (" + reason + ")");

        if (reason == TGPluginShutdownEvent.Reason.LOADER_RESTART
                || reason == TGPluginShutdownEvent.Reason.UPDATE_TRIGGERED) {
            hook();
        }
    }
}
