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

package com.deathmotion.totemguard.bungee;

import com.deathmotion.totemguard.bungee.compatibility.BungeeCompatibility;
import com.deathmotion.totemguard.bungee.player.BungeePlatformUserFactory;
import com.deathmotion.totemguard.bungee.scheduler.BungeeScheduler;
import com.deathmotion.totemguard.bungee.sender.BungeeSenderFactory;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformUserFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.Scheduler;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bungee.BungeeCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

@Getter
public class TGBungeePlatform extends TGPlatform {

    private final TGBungee plugin;
    private final Scheduler scheduler;

    private final Lazy<BungeeSenderFactory> senderFactory;
    private final Lazy<BungeePlatformUserFactory> platformUserFactory;
    private final Lazy<CommandManager<Sender>> commandManager;

    public TGBungeePlatform(TGBungee plugin) {
        super(Platform.BUNGEE);
        this.plugin = plugin;
        this.scheduler = new BungeeScheduler(plugin);

        this.senderFactory = Lazy.of(BungeeSenderFactory::new);
        this.platformUserFactory = Lazy.of(BungeePlatformUserFactory::new);

        this.commandManager = Lazy.of(() -> new BungeeCommandManager<>(
                plugin,
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
        ProxyServer proxy = plugin.getProxy();
        CommandSender console = proxy.getConsole();
        proxy.getPluginManager().dispatchCommand(console, command);
    }

    @Override
    public CommandManager<Sender> getCommandManager() {
        return commandManager.get();
    }

    @Override
    public void enableBStats() {
        plugin.enableBStats();
    }

    @Override
    public PlatformUserFactory getPlatformUserFactory() {
        return platformUserFactory.get();
    }

    @Override
    public String getPluginDirectory() {
        return plugin.getDataFolder().getAbsolutePath();
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getProxy().getVersion();
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return plugin.getProxy().getPluginManager().getPlugin(pluginName) != null;
    }

    @Override
    public void disablePlugin() {
        // BungeeCord has no per-plugin disable API. setEnabled(false) gates further work.
    }

    @Override
    public boolean checkPlatformCompatibility() {
        return BungeeCompatibility.check(getLogger(), getPlatformVersion());
    }
}
