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

package com.deathmotion.totemguard.discord.fabric;

import com.deathmotion.totemguard.discord.DiscordService;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DiscordFabricMod implements DedicatedServerModInitializer {
    private final FabricDiscordPlatform platform = new FabricDiscordPlatform();
    private final DiscordService service = new DiscordService(platform);

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(platform::setServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            platform.setServer(null);
            service.stop();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("tgdiscord")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.literal("reload").executes(context -> {
                            context.getSource().sendSystemMessage(Component.literal(
                                    "Reloading the TotemGuard Discord bot. Check the console for the result."));
                            service.reload();
                            return 1;
                        }))));

        service.start();
    }
}
