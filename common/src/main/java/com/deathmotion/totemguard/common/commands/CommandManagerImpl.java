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

package com.deathmotion.totemguard.common.commands;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.impl.*;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.TGVersions;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import java.util.HashSet;
import java.util.Set;

public class CommandManagerImpl {

    private final Set<String> registeredRoots = new HashSet<>();
    CommandManager<Sender> commandManager;

    public CommandManagerImpl() {
        this.commandManager = TGPlatform.getInstance().getCommandManager();
        registerCommands();
    }

    public void registerCommands() {
        new TotemGuardCommand().register(commandManager);
        new ProfileCommand().register(commandManager);
        new HistoryCommand().register(commandManager);
        new ClearHistoryCommand().register(commandManager);
        new MonitorCommand().register(commandManager);
        new ReloadCommand().register(commandManager);
        new AlertCommand().register(commandManager);
        new FocusCommand().register(commandManager);
        new UnfocusCommand().register(commandManager);
        new FollowCommand().register(commandManager);
        new UnfollowCommand().register(commandManager);

        // Only register on development builds
        if (TGVersions.CURRENT.snapshot()) {
            new DebugOverlayCommand().register(commandManager);
            new PhysicsDumpCommand().register(commandManager);
            new InventoryCommand().register(commandManager);
            new PlaceholderCommand().register(commandManager);
            new TesterCommand().register(commandManager);
            new TestBanAnimationCommand().register(commandManager);
        }

        new CheckCommand().register(commandManager);
        new TeleportCommand().register(commandManager);
        new TopCommand().register(commandManager);
        new StatsCommand().register(commandManager);

        new ShutdownCommand().register(commandManager);

        // Restart only makes sense when the loader is in play; without one, there's no
        // PluginRuntime to drive a restart. Updates are entirely a loader concern now
        // (see /tgloader stage|apply|rollout).
        if (TGPlatform.getInstance().isManagedByLoader()) {
            new RestartCommand().register(commandManager);
        }

        // Capture root names after registration so unregisterAll() can roll them back
        // cleanly on shutdown. The loader path depends on this to leave Paper's
        // CommandMap clean between hot-reloads.
        for (Command<Sender> command : commandManager.commands()) {
            registeredRoots.add(command.rootComponent().name());
        }
    }

    public void unregisterAll() {
        for (String root : registeredRoots) {
            try {
                commandManager.deleteRootCommand(root);
            } catch (Exception ignored) {
                // Cloud throws if the root is missing; ignore so partial teardown still proceeds.
            }
        }
        registeredRoots.clear();
    }
}
