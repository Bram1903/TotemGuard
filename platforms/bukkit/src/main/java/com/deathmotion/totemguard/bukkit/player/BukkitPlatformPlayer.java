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

package com.deathmotion.totemguard.bukkit.player;

import com.deathmotion.totemguard.bukkit.scheduler.BukkitScheduler;
import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class BukkitPlatformPlayer implements PlatformPlayer {

    private static final double FORCED_DAMAGE = 1_000.0;

    @Getter
    private final Player bukkitPlayer;
    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitPlatformPlayer(Player bukkitPlayer, Plugin plugin, BukkitScheduler scheduler) {
        this.bukkitPlayer = bukkitPlayer;
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return bukkitPlayer.hasPermission(permission);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        bukkitPlayer.sendMessage(message);
    }

    @Override
    public void kick(@NotNull Component reason) {
        scheduler.runForEntity(bukkitPlayer, () -> {
            if (bukkitPlayer.isOnline()) bukkitPlayer.kick(reason);
        }, null);
    }

    @Override
    public boolean isInSurvivalOrAdventure() {
        GameMode mode = bukkitPlayer.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    @Override
    public boolean isInvulnerable() {
        return bukkitPlayer.isInvulnerable();
    }

    @Override
    public String getWorldName() {
        World world = bukkitPlayer.getWorld();
        return world == null ? null : world.getName();
    }

    @Override
    public void teleport(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
        scheduler.runForEntity(bukkitPlayer, () -> {
            if (!bukkitPlayer.isOnline()) return;
            World world = Bukkit.getWorld(worldName);
            World destWorld = world != null ? world : bukkitPlayer.getWorld();
            Location destination = new Location(destWorld, x, y, z, yaw, pitch);
            bukkitPlayer.teleportAsync(destination);
        }, null);
    }

    @Override
    public void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted,
                                 @NotNull Runnable onDamageRefused) {
        scheduler.runForEntity(bukkitPlayer, () -> {
            if (!bukkitPlayer.isOnline()) {
                onDamageRefused.run();
                return;
            }

            PlayerInventory inventory = bukkitPlayer.getInventory();
            if (inventory.getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                // Authoritative server-side state disagrees with our PacketInventory — refuse
                // rather than risk killing the target.
                onDamageRefused.run();
                return;
            }

            ItemStack[] contents = Arrays.stream(inventory.getContents())
                    .map(item -> item == null ? null : item.clone())
                    .toArray(ItemStack[]::new);
            ItemStack cursor = bukkitPlayer.getOpenInventory().getCursor();
            ItemStack cursorSnapshot = cursor == null ? null : cursor.clone();
            double health = bukkitPlayer.getHealth();
            int foodLevel = bukkitPlayer.getFoodLevel();
            float saturation = bukkitPlayer.getSaturation();
            Collection<PotionEffect> effects = new ArrayList<>(bukkitPlayer.getActivePotionEffects());

            BukkitManualCheckHandle handle = new BukkitManualCheckHandle(
                    scheduler, bukkitPlayer, contents, cursorSnapshot,
                    health, foodLevel, saturation, effects
            );

            UUID targetUuid = bukkitPlayer.getUniqueId();
            DamageObserver observer = new DamageObserver(targetUuid);
            Bukkit.getPluginManager().registerEvents(observer, plugin);

            // Drop to half a heart first so any surviving damage reduction can't block the kill
            // and force the natural totem pop.
            bukkitPlayer.setHealth(Math.min(0.5, bukkitPlayer.getMaxHealth()));
            bukkitPlayer.damage(FORCED_DAMAGE);

            HandlerList.unregisterAll(observer);

            if (observer.applied) {
                onStarted.accept(handle);
            } else {
                handle.restore();
                onDamageRefused.run();
            }
        }, null);
    }

    @Override
    public @Nullable String clientBrandName() {
        try {
            return bukkitPlayer.getClientBrandName();
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            return null;
        }
    }

    @Override
    public void resyncInventoryToClient() {
        scheduler.runForEntity(bukkitPlayer, () -> {
            if (!bukkitPlayer.isOnline()) return;
            bukkitPlayer.updateInventory();
        }, null);
    }

    /**
     * Watches the target's next {@code EntityDamageEvent} to see whether our forced-damage
     * call actually landed. A canceled event — or one dropped to zero final damage by an
     * anticheat / damage-cap plugin — means the check can't proceed, and we'll restore
     * immediately without a window.
     */
    private static final class DamageObserver implements Listener {

        private final UUID target;
        boolean applied;

        private DamageObserver(UUID target) {
            this.target = target;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDamage(EntityDamageEvent event) {
            if (!event.getEntity().getUniqueId().equals(target)) return;
            if (event.isCancelled()) return;
            if (event.getFinalDamage() <= 0) return;
            applied = true;
        }
    }
}
