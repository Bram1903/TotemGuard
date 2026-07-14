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

package com.deathmotion.totemguard.paper;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.paper.compatibility.PaperCompatibility;
import com.deathmotion.totemguard.paper.event.NetherPortalListener;
import com.deathmotion.totemguard.paper.event.PluginUnregisterHook;
import com.deathmotion.totemguard.paper.metrics.TGMetrics;
import com.deathmotion.totemguard.paper.network.PaperCrossServerTeleportRouter;
import com.deathmotion.totemguard.paper.player.PaperPlatformPlayerFactory;
import com.deathmotion.totemguard.paper.scheduler.PaperScheduler;
import com.deathmotion.totemguard.paper.sender.PaperSenderFactory;
import com.deathmotion.totemguard.paper.tick.PaperTickRunner;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.Configurable;
import org.incendo.cloud.setting.ManagerSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


@Getter
public class TGPaperPlatform extends TGPlatform {

    private final JavaPlugin plugin;
    private final PaperScheduler scheduler;

    private final Lazy<PaperSenderFactory> senderFactory;
    private final Lazy<PaperPlatformPlayerFactory> platformPlayerFactory;
    private final Lazy<CommandManager<Sender>> commandManager;
    private final Lazy<PaperCrossServerTeleportRouter> teleportRouter;
    private final PaperTickRunner tickRunner;
    private boolean brigadierNeedsLifecyclePublish;
    private TGMetrics metrics;

    public TGPaperPlatform(JavaPlugin plugin) {
        super(Platform.PAPER);
        this.plugin = plugin;
        this.scheduler = PaperScheduler.create(plugin);
        this.tickRunner = new PaperTickRunner(this, scheduler);

        this.senderFactory = Lazy.of(() -> new PaperSenderFactory(scheduler));
        this.platformPlayerFactory = Lazy.of(() -> new PaperPlatformPlayerFactory(plugin, scheduler));

        this.teleportRouter = Lazy.of(() -> new PaperCrossServerTeleportRouter(this));

        this.commandManager = Lazy.of(() -> {
            LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                    plugin,
                    ExecutionCoordinator.simpleCoordinator(),
                    senderFactory.get()
            );

            if (isManagedByLoader()) {
                Configurable<ManagerSetting> managerSettings = manager.settings();
                managerSettings.set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);
                managerSettings.set(ManagerSetting.OVERRIDE_EXISTING_COMMANDS, true);
            }

            boolean brigadierRegistered = false;
            if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                if (!PaperLifecycleHack.isLifecycleRegistrationClosed(plugin)) {
                    manager.registerBrigadier();
                    brigadierRegistered = true;
                } else if (PaperLifecycleHack.forceRegisterBrigadier(plugin, manager, getLogger())) {
                    brigadierRegistered = true;
                    brigadierNeedsLifecyclePublish = true;
                } else {
                    getLogger().warning("Brigadier registration unavailable (lifecycle window is closed and the force-register path failed). Falling back to asynchronous tab completion.");
                }
            }
            if (brigadierRegistered) {
                CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
                Configurable<BrigadierSetting> settings = cbm.settings();
                settings.set(BrigadierSetting.FORCE_EXECUTABLE, true);
            } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                manager.registerAsynchronousCompletions();
            }

            return manager;
        });
    }

    @Override
    public PaperScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void dispatchCommand(String command) {
        scheduler.runMainThreadTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    @Override
    public @Nullable Sender createSender(@NotNull UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return null;
        return senderFactory.get().wrap(player);
    }

    @Override
    public CommandManager<Sender> getCommandManager() {
        return commandManager.get();
    }

    @Override
    public void enableBStats() {
        try {
            metrics = new TGMetrics(plugin, TGPlatform.getBStatsId(), TGVersions.CURRENT::toDisplayString);
        } catch (Exception e) {
            getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
        }
    }

    @Override
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return platformPlayerFactory.get();
    }

    @Override
    public String getPluginDirectory() {
        if (isManagedByLoader() && getPluginHost() != null) {
            return getPluginHost().dataFolder().toAbsolutePath().toString();
        }
        return plugin.getDataFolder().getAbsolutePath();
    }

    @Override
    public String getPlatformVersion() {
        return Bukkit.getMinecraftVersion();
    }

    @Override
    public boolean isPluginEnabled(String plugin) {
        return Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    @Override
    public void disablePlugin() {
        if (isManagedByLoader()) {
            setEnabled(false);
            return;
        }
        Bukkit.getPluginManager().disablePlugin(plugin);
    }

    @Override
    protected void afterCommandsRegistered() {
        if (brigadierNeedsLifecyclePublish) {
            brigadierNeedsLifecyclePublish = false;
            PaperLifecycleHack.publishCommandsToBrigadier(plugin, getLogger());
        }
    }

    @Override
    public void commonOnEnable() {
        super.commonOnEnable();
        if (!isEnabled()) return;
        Bukkit.getPluginManager().registerEvents(new PluginUnregisterHook(getEventBus(), plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new NetherPortalListener(), plugin);
        tickRunner.start();
    }

    @Override
    public void commonOnDisable() {
        super.commonOnDisable();
        tickRunner.stop();
        if (metrics != null) {
            try {
                metrics.shutdown();
            } catch (Exception ex) {
                getLogger().warning("Failed to shut down bStats: " + ex.getMessage());
            }
            metrics = null;
        }
        try {
            HandlerList.unregisterAll(plugin);
        } catch (Exception ex) {
            getLogger().warning("Failed to unregister listeners: " + ex.getMessage());
        }
        try {
            Bukkit.getScheduler().cancelTasks(plugin);
        } catch (Exception ex) {
            getLogger().warning("Failed to cancel scheduled tasks: " + ex.getMessage());
        }
    }

    @Override
    public boolean checkPlatformCompatibility() {
        return PaperCompatibility.check(getLogger());
    }

    @Override
    public void handleIncomingTeleportRequest(SyncTeleportRequestPacket.Payload payload) {
        teleportRouter.get().accept(payload);
    }
}
