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
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemA extends Check implements Listener {

    private final TotemGuard plugin;
    private final MessageService messageService;
    private final ConcurrentHashMap<UUID, Long> totemUsage;
    private final ConcurrentHashMap<UUID, Long> clickTimes;

    public AutoTotemA(TotemGuard plugin) {
        super(plugin, "AutoTotemA", "Click time difference");

        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.totemUsage = new ConcurrentHashMap<>();
        this.clickTimes = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) return;

        if (!player.getInventory().containsAtLeast(new ItemStack(Material.TOTEM_OF_UNDYING), 2)) {
            return;
        }

        totemUsage.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() == 45 && event.getCursor().getType() == Material.TOTEM_OF_UNDYING) {
            clickTimes.computeIfPresent(player.getUniqueId(), (uuid, clickTime) -> {
                checkSuspiciousActivity(player, clickTime);
                return clickTime;
            });

            return;
        }

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            clickTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Override
    public void resetData() {
        super.resetData();
        totemUsage.clear();
        clickTimes.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        totemUsage.remove(uuid);
        clickTimes.remove(uuid);
    }

    private void checkSuspiciousActivity(Player player, long clickTime) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            Long usageTime = totemUsage.remove(player.getUniqueId());
            if (usageTime == null) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long timeDifference = Math.abs(currentTime - usageTime);
            long clickTimeDifference = Math.abs(currentTime - clickTime);
            long realTotemTime = Math.abs(timeDifference - player.getPing());

            var checkSettings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemA();

            if (clickTimeDifference <= checkSettings.getClickTimeDifference() && timeDifference <= checkSettings.getNormalCheckTimeMs()) {
                flag(player, createDetails(timeDifference, realTotemTime, clickTimeDifference, player), checkSettings);
            }
        });
    }

    private String getMainHandItemString(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : player.getInventory().getItemInMainHand().getType().toString();
    }

    private Component createDetails(long timeDifference, long realTotemTime, long clickTimeDifference, Player player) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        Component component = Component.text()
                .append(Component.text("Totem Time: ", colorScheme.getY()))
                .append(Component.text(timeDifference + "ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Real Totem Time: ", colorScheme.getY()))
                .append(Component.text(realTotemTime + "ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Click Difference: ", colorScheme.getY()))
                .append(Component.text(clickTimeDifference + "ms", colorScheme.getX()))
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