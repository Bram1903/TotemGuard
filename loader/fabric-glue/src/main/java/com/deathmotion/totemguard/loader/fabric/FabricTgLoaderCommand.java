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

package com.deathmotion.totemguard.loader.fabric;

import com.deathmotion.totemguard.loader.command.LoaderCommandService;
import com.deathmotion.totemguard.loader.command.LoaderMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class FabricTgLoaderCommand {

    private final TGLoaderFabric loader;
    private final LoaderCommandService service;

    public FabricTgLoaderCommand(TGLoaderFabric loader) {
        this.loader = loader;
        this.service = new LoaderCommandService(loader);
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
                dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("tgloader")
                        .executes(ctx -> {
                            handle(ctx.getSource(), new String[0]);
                            return 0;
                        })
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(
                                        "args", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "args");
                                    handle(ctx.getSource(), raw.isBlank() ? new String[0] : raw.split("\\s+"));
                                    return 0;
                                }))));
        loader.logger().info("/tgloader registered (console-only).");
    }

    private void handle(CommandSourceStack source, String[] args) {
        // Loader has no permission integration, so we gate on "no attached Entity".
        // The dedicated-server console is a CommandSourceStack with a null entity.
        if (source.getEntity() != null) {
            source.sendSuccess(() -> Component.literal("Only the console can run /tgloader."), false);
            return;
        }
        LoaderMessage.Sink sink = line -> source.sendSuccess(() -> Component.literal(line.plain()), false);
        service.dispatch(sink, args);
    }
}
