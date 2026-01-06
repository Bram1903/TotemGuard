// Copied straight from Grim / Axionize as it looks well implemented, and I am not trying to reinvent the wheel

package com.deathmotion.totemguard.common.platform.sender;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Simple implementation of {@link Sender} using a {@link SenderFactory}
 *
 * @param <T> the command sender type
 */
public final class AbstractSender<T> implements Sender {
    private final SenderFactory<T> factory;
    private final T sender;

    private final UUID uniqueId;
    private final String name;
    private final boolean isConsole;

    AbstractSender(@NotNull SenderFactory<T> factory, @NotNull T sender) {
        this.factory = factory;
        this.sender = sender;
        this.uniqueId = factory.getUniqueId(this.sender);
        this.name = factory.getName(this.sender);
        this.isConsole = factory.isConsole(this.sender);
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void sendMessage(String message) {
        this.factory.sendMessage(this.sender, message);
    }

    @Override
    public void sendMessage(Component message) {
        this.factory.sendMessage(this.sender, message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return isConsole() || this.factory.hasPermission(this.sender, permission);
    }

    @Override
    public boolean hasPermission(String permission, boolean defaultIfUnset) {
        return isConsole() || this.factory.hasPermission(this.sender, permission, defaultIfUnset);
    }

    @Override
    public void performCommand(String commandLine) {
        this.factory.performCommand(this.sender, commandLine);
    }

    @Override
    public boolean isConsole() {
        return this.isConsole;
    }

    @Override
    public boolean isPlayer() {
        return this.factory.isPlayer(sender);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractSender)) {
            return false;
        }
        AbstractSender<?> that = (AbstractSender<?>) o;
        return this.uniqueId.equals(that.uniqueId);
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public @NotNull T getNativeSender() {
        return sender;
    }

    @Override
    public @Nullable TGPlayer getTGPlayer() {
        return TGPlatform.getInstance().getPlayerRepository().getPlayer(this.uniqueId);
    }
}
