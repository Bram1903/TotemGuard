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
import org.incendo.cloud.CommandManager;

public class CommandManagerImpl {

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

        // Only register on development builds
        if (TGVersions.CURRENT.snapshot()) {
            new DebugOverlayCommand().register(commandManager);
            new InventoryCommand().register(commandManager);
            new PlaceholderCommand().register(commandManager);
            new TesterCommand().register(commandManager);
            new TestBanAnimationCommand().register(commandManager);
        }

        new CheckCommand().register(commandManager);
        new TeleportCommand().register(commandManager);
        new TopCommand().register(commandManager);
        new StatsCommand().register(commandManager);
    }
}
