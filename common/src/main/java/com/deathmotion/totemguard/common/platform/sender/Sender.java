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

// Copied straight from Grim / Axionize as it looks well implemented, and I am not trying to reinvent the wheel

package com.deathmotion.totemguard.common.platform.sender;

import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Wrapper interface to represent a CommandSender/CommandSource within the common command implementations.
 */
public interface Sender {

    /**
     * The uuid used by the console sender.
     */
    UUID CONSOLE_UUID = new UUID(0, 0); // 00000000-0000-0000-0000-000000000000

    /**
     * The name used by the console sender.
     */
    String CONSOLE_NAME = "Console";

    /**
     * Gets the sender's username
     *
     * @return a friendly username for the sender
     */
    String getName();

    /**
     * Gets the sender's unique id.
     *
     * <p>See {@link #CONSOLE_UUID} for the console's UUID representation.</p>
     *
     * @return the sender's uuid
     */
    UUID getUniqueId();

    /**
     * Send a json message to the Sender.
     *
     * @param message the message to send.
     */
    void sendMessage(String message);

    /**
     * Send a component message to the Sender.
     *
     * @param message the component message to send.
     */
    void sendMessage(Component message);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission the permission to check for
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission     the permission to check for
     * @param defaultIfUnset the default value of the permission, if not yet set.
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission, boolean defaultIfUnset);

    /**
     * Makes the sender perform a command.
     *
     * @param commandLine the command
     */
    void performCommand(String commandLine);

    /**
     * Gets whether this sender is the console
     *
     * @return if the sender is the console
     */
    boolean isConsole();

    /**
     * Gets whether this sender is a player
     *
     * @return if the sender is a player
     */
    boolean isPlayer();

    /**
     * Gets whether this sender is still valid and receiving messages.
     *
     * @return if this sender is valid
     */
    default boolean isValid() {
        return true;
    }

    /**
     * Gets the native platform-specific command sender object.
     *
     * @return The platform's native command sender type:
     * <ul>
     * <li>Bukkit/Spigot/Paper/Folia/Pufferfish/etc... {@code org.bukkit.command.CommandSender}</li>
     * <li>Fabric:
     *     <ul>
     *     <li>Yarn: {@code net.minecraft.server.command.ServerCommandSource}</li>
     *     <li>Mojmap: {@code net.minecraft.commands.CommandSourceStack}</li>
     *     </ul>
     * <li>Velocity: {@code com.velocitypowered.api.command.CommandSource}</li>
     * <li>BungeeCord: {@code net.md_5.bungee.api.CommandSender}</li>
     * <li>Sponge: {@code org.spongepowered.api.command.CommandCause}</li>
     * <li>Forge/NeoForge: {@code net.minecraft.commands.CommandSourceStack}</li>
     * </ul>
     */
    @NotNull Object getNativeSender();

    /**
     * Gets the PlatformUser tied to a sender
     *
     * @return PlatformUser wrapping the underlying native platform-specific user type, null if Sender is not a user
     */
    @Nullable PlatformUser getPlatformUser();

    /**
     * Gets the PlatformPlayer tied to a sender
     *
     * @return PlatformPlayer wrapping the underlying native platform-specific player type, null if Sender is not a player
     */
    @Nullable PlatformPlayer getPlatformPlayer();
}
