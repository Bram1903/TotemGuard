/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.commands.cloud.impl;

import com.deathmotion.totemguard.commands.cloud.BuildableCommand;
import lombok.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.Source;

public class HelloWorldCommand implements BuildableCommand {

    @Override
    public void register(PaperCommandManager<Source> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("totemguard", "tg")
                        .literal("hello", Description.of("Says hello world"))
                        .permission("TotemGuard.Hello")
                        .handler(this::handleHelloWorldCommand)
        );
    }

    private void handleHelloWorldCommand(@NonNull CommandContext<Source> context) {
        Source sender = context.sender();
        sender.source().sendMessage("Hello world!");
    }
}
