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

import com.deathmotion.totemguard.paper.compatibility.PaperCompatibility;
import com.deathmotion.totemguard.paper.metrics.TGMetrics;
import com.deathmotion.totemguard.paper.network.PaperCrossServerTeleportRouter;
import com.deathmotion.totemguard.paper.player.PaperPlatformPlayerFactory;
import com.deathmotion.totemguard.paper.scheduler.PaperScheduler;
import com.deathmotion.totemguard.paper.sender.PaperSenderFactory;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.TGVersions;
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

    private boolean brigadierNeedsLifecyclePublish;
    private TGMetrics metrics;

    public TGPaperPlatform(JavaPlugin plugin) {
        super(Platform.PAPER);
        this.plugin = plugin;
        this.scheduler = PaperScheduler.create(plugin);

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
                // Hot-reload runs after Cloud's normal registration window has closed and
                // after Paper's command map already holds the previous restart's /tg entries.
                // Opt into unsafe registration + override so we can rebuild the command tree.
                Configurable<ManagerSetting> managerSettings = manager.settings();
                managerSettings.set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);
                managerSettings.set(ManagerSetting.OVERRIDE_EXISTING_COMMANDS, true);
            }

            boolean brigadierRegistered = false;
            if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                // Paper closes the lifecycle event registration window once the server has
                // finished booting. On cold-start (loader or standalone) we are still inside
                // it, so registerBrigadier() works directly. On a loader-driven hot reload we
                // are past the window; force re-registration via Paper internals.
                if (!PaperLifecycleHack.isLifecycleRegistrationClosed(plugin)) {
                    manager.registerBrigadier();
                    brigadierRegistered = true;
                } else if (PaperLifecycleHack.forceRegisterBrigadier(plugin, manager, getLogger())) {
                    brigadierRegistered = true;
                    // The lifecycle event won't fire naturally now. Defer the publish until
                    // CommandManagerImpl has populated Cloud's command tree, otherwise the
                    // handler would run against an empty tree and only the root literals
                    // would land in brigadier.
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
        // Under the loader the inner plugin's data folder is pinned to plugins/TotemGuard/
        // (see LoaderPaths.forPaper) and lives independently of the loader plugin's own
        // data folder, which Paper derives from the loader's plugin.yml name. Reading
        // plugin.getDataFolder() here would point at the loader's folder and miss the
        // user's configs.
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
    public void commonOnDisable() {
        super.commonOnDisable();
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
