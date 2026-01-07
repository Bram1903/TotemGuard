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
import com.deathmotion.totemguard.common.commands.impl.AlertCommand;
import com.deathmotion.totemguard.common.commands.impl.InventoryCommand;
import com.deathmotion.totemguard.common.commands.impl.PlaceholderCommand;
import com.deathmotion.totemguard.common.commands.impl.TestCommand;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import org.incendo.cloud.CommandManager;

public class CommandManagerImpl {

    CommandManager<Sender> commandManager;

    public CommandManagerImpl() {
        this.commandManager = TGPlatform.getInstance().getCommandManager();
        registerCommands();
    }

    public void registerCommands() {
        new AlertCommand().register(commandManager);
        new TestCommand().register(commandManager);
        new InventoryCommand().register(commandManager);
        new PlaceholderCommand().register(commandManager);
    }
}
