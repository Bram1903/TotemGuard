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

package com.deathmotion.totemguard.paper.player;

import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.paper.scheduler.PaperScheduler;
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

public class PaperPlatformPlayer implements PlatformPlayer {

    private static final double FORCED_DAMAGE = 1_000.0;

    @Getter
    private final Player paperPlayer;
    private final Plugin plugin;
    private final PaperScheduler scheduler;

    public PaperPlatformPlayer(Player paperPlayer, Plugin plugin, PaperScheduler scheduler) {
        this.paperPlayer = paperPlayer;
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return paperPlayer.hasPermission(permission);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        paperPlayer.sendMessage(message);
    }

    @Override
    public void kick(@NotNull Component reason) {
        scheduler.runForEntity(paperPlayer, () -> {
            if (paperPlayer.isOnline()) paperPlayer.kick(reason);
        }, null);
    }

    @Override
    public boolean isInSurvivalOrAdventure() {
        GameMode mode = paperPlayer.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    @Override
    public boolean isInvulnerable() {
        return paperPlayer.isInvulnerable();
    }

    @Override
    public String getWorldName() {
        World world = paperPlayer.getWorld();
        return world == null ? null : world.getName();
    }

    @Override
    public void teleport(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
        scheduler.runForEntity(paperPlayer, () -> {
            if (!paperPlayer.isOnline()) return;
            World world = Bukkit.getWorld(worldName);
            World destWorld = world != null ? world : paperPlayer.getWorld();
            Location destination = new Location(destWorld, x, y, z, yaw, pitch);
            paperPlayer.teleportAsync(destination);
        }, null);
    }

    @Override
    public void stopRiding() {
        scheduler.runForEntity(paperPlayer, () -> {
            if (!paperPlayer.isOnline()) return;
            paperPlayer.leaveVehicle();
        }, null);
    }

    @Override
    public void resetFallDistance() {
        scheduler.runForEntity(paperPlayer, () -> {
            if (!paperPlayer.isOnline()) return;
            paperPlayer.setFallDistance(0.0F);
        }, null);
    }

    @Override
    public boolean dealFallDamage(double amount) {
        if (amount <= 0.0) return false;
        scheduler.runForEntity(paperPlayer, () -> {
            if (!paperPlayer.isOnline() || paperPlayer.isDead()) return;
            FallDamageSupport.damage(paperPlayer, amount);
        }, null);
        return true;
    }

    @Override
    public void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted,
                                 @NotNull Runnable onDamageRefused) {
        scheduler.runForEntity(paperPlayer, () -> {
            if (!paperPlayer.isOnline()) {
                onDamageRefused.run();
                return;
            }

            PlayerInventory inventory = paperPlayer.getInventory();
            if (inventory.getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                // Authoritative server-side state disagrees with our PacketInventory, refuse
                // rather than risk killing the target.
                onDamageRefused.run();
                return;
            }

            ItemStack[] contents = Arrays.stream(inventory.getContents())
                    .map(item -> item == null ? null : item.clone())
                    .toArray(ItemStack[]::new);
            ItemStack cursor = paperPlayer.getOpenInventory().getCursor();
            ItemStack cursorSnapshot = cursor == null ? null : cursor.clone();
            double health = paperPlayer.getHealth();
            int foodLevel = paperPlayer.getFoodLevel();
            float saturation = paperPlayer.getSaturation();
            Collection<PotionEffect> effects = new ArrayList<>(paperPlayer.getActivePotionEffects());

            PaperManualCheckHandle handle = new PaperManualCheckHandle(
                    scheduler, paperPlayer, contents, cursorSnapshot,
                    health, foodLevel, saturation, effects
            );

            UUID targetUuid = paperPlayer.getUniqueId();
            DamageObserver observer = new DamageObserver(targetUuid);
            Bukkit.getPluginManager().registerEvents(observer, plugin);

            // Drop to half a heart first so any surviving damage reduction can't block the kill
            // and force the natural totem pop.
            paperPlayer.setHealth(Math.min(0.5, paperPlayer.getMaxHealth()));
            paperPlayer.damage(FORCED_DAMAGE);

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
            return paperPlayer.getClientBrandName();
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            return null;
        }
    }

    @Override
    public void resyncInventoryToClient() {
        scheduler.runForEntity(paperPlayer, () -> {
            if (!paperPlayer.isOnline()) return;
            paperPlayer.updateInventory();
        }, null);
    }

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
