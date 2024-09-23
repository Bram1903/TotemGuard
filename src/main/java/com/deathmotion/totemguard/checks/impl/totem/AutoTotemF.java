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
import com.deathmotion.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemF extends Check implements Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Long> invClick;

    public AutoTotemF(TotemGuard plugin) {
        super(plugin, "AutoTotemF", "Invalid interaction with open inventory", true);
        this.plugin = plugin;

        this.invClick = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        EnumSet<ClickType> validClickTypes = EnumSet.of(
                ClickType.LEFT,
                ClickType.RIGHT,
                ClickType.DOUBLE_CLICK,
                ClickType.SWAP_OFFHAND,
                ClickType.NUMBER_KEY
        );

        if (!validClickTypes.contains(event.getClick())) return;

        plugin.debug("Click Type: " + event.getClick() + " (" + player.getName() + ")");

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
        invClick.remove(event.getPlayer().getUniqueId());
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

        checkSuspiciousActivity(player, storedTime, String.valueOf(action));
    }

    private void checkSuspiciousActivity(Player player, long storedTime, String action){
        long timeDifference = Math.abs(System.currentTimeMillis() - storedTime);

        plugin.debug("Time difference: " + timeDifference + "ms (" + player.getName() + ")");

        if (timeDifference <= 1500){
            final Settings.Checks.AutoTotemF settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemF();
            flag(player, createDetails(Action.valueOf(action), timeDifference, player), settings);
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
        invClick.clear();
    }

    private String getMainHandItemString(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : player.getInventory().getItemInMainHand().getType().toString();
    }

    private Component createDetails(Action action, Long timeDifference, Player player) {
        Component component = Component.text()
                .append(Component.text("Type: ", NamedTextColor.GRAY))
                .append(Component.text(action.toString(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Time Difference: ", NamedTextColor.GRAY))
                .append(Component.text(timeDifference, NamedTextColor.GOLD))
                .append(Component.text("ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", NamedTextColor.GRAY))
                .append(Component.text(getMainHandItemString(player), NamedTextColor.GOLD))
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
            component = component.append(Component.text("States: ", NamedTextColor.GRAY))
                    .append(Component.text(states.toString(), NamedTextColor.GOLD));
        }

        return component;
    }
}
