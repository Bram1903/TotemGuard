// Copied straight from Grim / Axionize as it looks well implemented, and I am not trying to reinvent the wheel

package com.deathmotion.totemguard.bukkit.sender;

import com.deathmotion.totemguard.bukkit.TGBukkit;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.platform.sender.SenderFactory;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.incendo.cloud.SenderMapper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BukkitSenderFactory extends SenderFactory<CommandSender> implements SenderMapper<CommandSender, Sender> {

    @Override
    protected String getName(CommandSender sender) {
        return sender instanceof Player ? sender.getName() : Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        // we can safely send async for players and the console - otherwise, send it sync
        if (sender instanceof Player || sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) {
            sender.sendMessage(message);
        } else {
            FoliaScheduler.getGlobalRegionScheduler().run(TGBukkit.getInstance(), (o) -> sender.sendMessage(message));
        }
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node, boolean defaultIfUnset) {
        return sender.hasPermission(new Permission(node, defaultIfUnset ? PermissionDefault.TRUE : PermissionDefault.FALSE));
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }

    @Override
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    @Override
    public @NotNull Sender map(@NotNull CommandSender base) {
        return this.wrap(base);
    }

    @Override
    public @NotNull CommandSender reverse(@NotNull Sender mapped) {
        return this.unwrap(mapped);
    }
}
