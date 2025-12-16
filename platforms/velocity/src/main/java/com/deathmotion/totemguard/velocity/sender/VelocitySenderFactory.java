package com.deathmotion.totemguard.velocity.sender;

import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.platform.sender.SenderFactory;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.SenderMapper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class VelocitySenderFactory extends SenderFactory<CommandSource> implements SenderMapper<CommandSource, Sender> {

    @Override
    protected String getName(final CommandSource sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUsername();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(final CommandSource sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(final CommandSource sender, final String message) {
        sender.sendPlainMessage(message);
    }

    @Override
    protected void sendMessage(final CommandSource sender, final Component message) {
        sender.sendMessage(message);
    }

    @Override
    protected boolean hasPermission(final CommandSource sender, final String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected boolean hasPermission(final CommandSource sender, final String node, final boolean defaultIfUnset) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(final CommandSource sender, final String command) {
        throw new UnsupportedOperationException("performCommand is not implemented for Velocity");
    }

    @Override
    protected boolean isConsole(final CommandSource sender) {
        return sender instanceof ConsoleCommandSource;
    }

    @Override
    protected boolean isPlayer(final CommandSource sender) {
        return sender instanceof Player;
    }

    @Override
    public @NotNull Sender map(final @NotNull CommandSource base) {
        return this.wrap(base);
    }

    @Override
    public @NotNull CommandSource reverse(final @NotNull Sender mapped) {
        return this.unwrap(mapped);
    }
}
