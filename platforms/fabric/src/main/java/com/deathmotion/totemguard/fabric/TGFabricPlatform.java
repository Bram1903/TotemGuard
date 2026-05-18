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

package com.deathmotion.totemguard.fabric;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.fabric.compatibility.FabricCompatibility;
import com.deathmotion.totemguard.fabric.player.FabricPlatformPlayerFactory;
import com.deathmotion.totemguard.fabric.scheduler.FabricScheduler;
import com.deathmotion.totemguard.fabric.sender.FabricSenderFactory;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;

@Getter
public class TGFabricPlatform extends TGPlatform {

    private final Path configDirectory;
    private final Scheduler scheduler;

    private final Lazy<FabricPlatformPlayerFactory> platformPlayerFactory;
    private final Lazy<FabricSenderFactory> senderFactory;
    private final Lazy<CommandManager<Sender>> commandManager;

    public TGFabricPlatform(Path configDirectory) {
        super(Platform.FABRIC);
        this.configDirectory = configDirectory;
        this.scheduler = new FabricScheduler();
        this.platformPlayerFactory = Lazy.of(FabricPlatformPlayerFactory::new);
        this.senderFactory = Lazy.of(FabricSenderFactory::new);
        this.commandManager = Lazy.of(() -> new FabricServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        ));
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void dispatchCommand(String command) {
        MinecraftServer server = FabricServerHolder.server();
        if (server == null) return;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    @Override
    public @Nullable Sender createSender(@NotNull UUID playerUuid) {
        MinecraftServer server = FabricServerHolder.server();
        if (server == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return null;
        return senderFactory.get().wrap(player.createCommandSourceStack());
    }

    @Override
    public CommandManager<Sender> getCommandManager() {
        return commandManager.get();
    }

    @Override
    public void enableBStats() {
        // bstats has no Fabric module. Skip.
    }

    @Override
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return platformPlayerFactory.get();
    }

    @Override
    public String getPluginDirectory() {
        return configDirectory.toAbsolutePath().toString();
    }

    @Override
    public String getPlatformVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return FabricLoader.getInstance().isModLoaded(pluginName);
    }

    @Override
    public void disablePlugin() {

    }

    @Override
    public boolean checkPlatformCompatibility() {
        return FabricCompatibility.check(getLogger(), getPlatformVersion());
    }

    @Override
    public boolean shouldVerifyJarIntegrity() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            getLogger().info("Dev environment detected, skipping plugin jar integrity verification.");
            return false;
        }
        return true;
    }
}
