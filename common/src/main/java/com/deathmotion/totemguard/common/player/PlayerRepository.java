package com.deathmotion.totemguard.common.player;

import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerRepository implements UserRepository {

    private static final String BYPASS_PERMISSION = "TotemGuard.Bypass";

    private final TGPlatform platform = TGPlatform.getInstance();

    private final ConcurrentMap<UUID, TGPlayer> players = new ConcurrentHashMap<>();
    private final Collection<UUID> exemptUsers = ConcurrentHashMap.newKeySet();

    public void onLoginPacket(final @NotNull User user) {
        if (!shouldCheck(user, null)) return;

        final UUID uuid = user.getUUID();
        if (uuid == null) return;

        players.put(uuid, new TGPlayer(user));
    }

    public void onLogin(final @NotNull User user) {
        final TGPlayer player = players.get(user.getUUID());
        if (player != null) {
            player.onLogin();
        }
    }

    public void onPlayerDisconnect(final @NotNull User user) {
        UUID uuid = user.getUUID();
        if (uuid == null) return;

        clearExempt(uuid);

        final TGPlayer player = players.remove(uuid);
        if (player == null) return;

        platform.getEventRepository().post(new TGUserQuitEvent(player));
    }

    public void removeUser(final @NotNull UUID uuid) {
        players.remove(uuid);
        clearExempt(uuid);
    }

    public @Nullable TGPlayer getPlayer(final @NotNull UUID uuid) {
        return players.get(uuid);
    }

    public boolean isExempt(final @NotNull UUID uuid) {
        return exemptUsers.contains(uuid);
    }

    public void setExempt(final @NotNull UUID uuid, final boolean exempt) {
        if (exempt) exemptUsers.add(uuid);
        else clearExempt(uuid);
    }

    private void clearExempt(final @NotNull UUID uuid) {
        exemptUsers.remove(uuid);
    }

    public boolean shouldCheck(final @NotNull User user, final @Nullable PlatformUser platformUser) {
        final UUID uuid = user.getUUID();
        if (uuid == null) return false;

        if (isExempt(uuid)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (uuid.getMostSignificantBits() == 0L) {
            setExempt(uuid, true);
            return false;
        }

        if (platformUser != null && platformUser.hasPermission(BYPASS_PERMISSION)) {
            setExempt(uuid, true);
            return false;
        }

        return true;
    }

    @Override
    public @Nullable TGUser getUser(final @NotNull String uuid) {
        final UUID parsed;
        try {
            parsed = UUID.fromString(uuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        return players.get(parsed);
    }
}
