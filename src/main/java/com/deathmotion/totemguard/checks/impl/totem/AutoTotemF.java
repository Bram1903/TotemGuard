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

package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.impl.Checks;
import com.deathmotion.totemguard.models.ValidClickTypes;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemF extends Check implements Listener {

    private final TotemGuard plugin;
    private final MessageService messageService;
    private final ConcurrentHashMap<UUID, Long> invClick;

    public AutoTotemF(TotemGuard plugin) {
        super(plugin, "AutoTotemF", "Invalid interaction", true);
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();

        this.invClick = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inventory = event.getClickedInventory();
        if (inventory == null || inventory.getType() != InventoryType.PLAYER) return;
        if (!ValidClickTypes.isClickTypeValid((event.getClick()))) return;

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            invClick.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        invClick.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        invClick.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!invClick.containsKey(player.getUniqueId())) return;

        long storedTime = invClick.get(playerId);
        invClick.remove(playerId);
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;

        checkSuspiciousActivity(player, storedTime, action);
    }

    private void checkSuspiciousActivity(Player player, long storedTime, Action action) {
        long timeDifference = Math.abs(System.currentTimeMillis() - storedTime);

        final Checks.AutoTotemF settings = plugin.getConfigManager().getChecks().getAutoTotemF();
        if (timeDifference <= settings.getTimeDifference()) {
            flag(player, createDetails(action, timeDifference, player), settings);
        }
    }

    @Override
    public void resetData() {
        super.resetData();
        invClick.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        invClick.remove(uuid);
    }

    private String getMainHandItemString(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : player.getInventory().getItemInMainHand().getType().toString();
    }

    private Component createDetails(Action action, Long timeDifference, Player player) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        Component component = Component.text()
                .append(Component.text("Type: ", colorScheme.getY()))
                .append(Component.text(action.toString(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Time Difference: ", colorScheme.getY()))
                .append(Component.text(timeDifference, colorScheme.getX()))
                .append(Component.text("ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", colorScheme.getY()))
                .append(Component.text(getMainHandItemString(player), colorScheme.getX()))
                .append(Component.newline())
                .build();

        StringBuilder states = new StringBuilder();
        if (player.isSprinting()) {
            states.append("Sprinting, ");
        }
        if (player.isSneaking()) {
            states.append("Sneaking, ");
        }
        if (player.isBlocking()) {
            states.append("Blocking, ");
        }

        // If any states are active, add them to the component
        if (!states.isEmpty()) {
            states.setLength(states.length() - 2);
            component = component.append(Component.text("States: ", colorScheme.getY()))
                    .append(Component.text(states.toString(), colorScheme.getX()));
        }

        return component;
    }
}
