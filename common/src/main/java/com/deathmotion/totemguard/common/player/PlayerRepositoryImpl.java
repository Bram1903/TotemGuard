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

package com.deathmotion.totemguard.common.player;

import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.FocusTarget;
import com.deathmotion.totemguard.common.commands.impl.FocusCommand;
import com.deathmotion.totemguard.common.commands.impl.TesterCommand;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.database.model.StaffAlertPref;
import com.deathmotion.totemguard.common.features.alert.AlertFilter;
import com.deathmotion.totemguard.common.features.alert.RealtimeAlertRoster;
import com.deathmotion.totemguard.common.features.update.UpdateCheckerRepositoryImpl;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.latency.TransactionTimeoutWatchdog;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerRepositoryImpl implements UserRepository {

    private static final String BYPASS_PERMISSION = "TotemGuard.Bypass";
    private static final Duration ALERTS_TOGGLE_TTL = Duration.ofMinutes(30);
    private final TGPlatform platform;
    private final CacheRepositoryImpl cacheRepository;
    private final ConcurrentMap<User, TGPlayer> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TGPlayer> playersByUuid = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ExemptPresence> exemptOnline = new ConcurrentHashMap<>();
    private final Collection<UUID> exemptUsers = ConcurrentHashMap.newKeySet();
    private final TransactionTimeoutWatchdog transactionTimeoutWatchdog;

    public PlayerRepositoryImpl() {
        platform = TGPlatform.getInstance();
        cacheRepository = platform.getCacheRepository();
        transactionTimeoutWatchdog = new TransactionTimeoutWatchdog(this);
        transactionTimeoutWatchdog.start();
    }

    public void shutdown() {
        transactionTimeoutWatchdog.stop();
    }

    public void persistAllOnShutdown() {
        if (!platform.getCacheRepository().isDistributed()) return;

        int persisted = 0;
        for (TGPlayer player : players.values()) {
            try {
                player.persistCacheOnShutdown();
                persisted++;
            } catch (Throwable t) {
                platform.getLogger().warning("Failed to persist shutdown cache for "
                        + player.getUser().getName() + ": " + t.getMessage());
            }
        }
        if (persisted > 0) {
            platform.getLogger().info("Persisted shutdown cache for " + persisted
                    + " player" + (persisted == 1 ? "" : "s") + ".");
        }
    }

    public void onLoginPacket(final @NotNull User user) {
        if (!shouldCheck(user, null)) return;
        TGPlayer player = new TGPlayer(user);
        players.put(user, player);
        UUID uuid = user.getUUID();
        if (uuid != null) {
            playersByUuid.put(uuid, player);
        }
    }

    public void onLogin(final @Nullable User user) {
        // Some fake-player plugins (e.g. Asteroid) fire UserLoginEvent with a null user
        // This is normally not possible, but not much I can do about it, so just ignore these events
        if (user == null) return;

        final UUID uuid = user.getUUID();
        if (uuid == null) {
            removeUser(user);
            return;
        }

        TGPlayer player = players.get(user);

        if (player == null) {
            if (!shouldCheck(user, null)) return;
            player = new TGPlayer(user);
            players.put(user, player);
            playersByUuid.put(uuid, player);
        } else {
            playersByUuid.putIfAbsent(uuid, player);
        }

        player.onLogin();
        PlatformPlayer platformPlayer = player.getPlatformPlayer();
        if (platformPlayer == null) return;

        restoreSubscriptions(uuid, platformPlayer);
        boolean bypassed = isExempt(uuid);
        if (bypassed) {
            exemptOnline.put(uuid, new ExemptPresence(user.getName(), user.getProfile()));
        }
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence != null) {
            presence.onLocalPlayerJoin(uuid, user.getName(), user.getProfile(), bypassed);
        }
        UpdateCheckerRepositoryImpl updateChecker = platform.getUpdateCheckerRepository();
        if (updateChecker != null) updateChecker.notifyIfOutdated(platformPlayer);
    }

    private void restoreSubscriptions(UUID uuid, PlatformPlayer platformPlayer) {
        boolean wantsAlerts = platformPlayer.hasPermission("TotemGuard.Alerts");
        boolean canFocus = platformPlayer.hasPermission("TotemGuard.Focus");
        boolean wantsTester = TGVersions.CURRENT.snapshot()
                && platformPlayer.hasPermission("TotemGuard.Tester")
                && !platformPlayer.hasPermission(BYPASS_PERMISSION);
        if (!wantsAlerts && !wantsTester && !canFocus) return;

        platform.getScheduler().runAsyncTask(() -> {
            if (wantsAlerts) {
                StaffAlertPref pref = resolveStaffAlertPref(uuid);
                platform.getAlertRepository().primeLocalOnly(uuid, pref.localOnly());
                if (pref.enabled()) {
                    platform.getAlertRepository().toggleAlerts(uuid);
                }
            }

            RealtimeAlertRoster roster = platform.getAlertRepository().getRealtimeRoster();
            boolean focusRestored = canFocus && tryRestoreFocus(uuid, platformPlayer, roster);

            if (!focusRestored && wantsTester && resolveTesterEnabled(uuid)) {
                if (roster.get(uuid) == null) {
                    roster.put(uuid, platformPlayer, new AlertFilter.Self(uuid), null);
                    platformPlayer.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TESTER_ENABLED));
                }
            }
        });
    }

    private boolean tryRestoreFocus(UUID viewerUuid, PlatformPlayer viewer, RealtimeAlertRoster roster) {
        FocusTarget cached = cacheRepository.getAndRefresh(
                CacheKeys.focusTarget(viewerUuid), CacheCodecs.FOCUS_TARGET, FocusCommand.FOCUS_TTL);
        if (cached == null) return false;

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        RemotePlayerEntry target = presence == null ? null : presence.findByUuid(cached.targetUuid());
        if (target == null) {
            cacheRepository.remove(CacheKeys.focusTarget(viewerUuid));
            return false;
        }

        TGPlayer localTarget = platform.getPlayerRepository().getPlayer(target.playerUuid());
        if (platform.getEventBus().getFocus().fireEnabling(
                viewerUuid, target.playerUuid(), target.playerName(), localTarget,
                target.serverInstanceId(), target.serverName(), true)) {
            cacheRepository.remove(CacheKeys.focusTarget(viewerUuid));
            return false;
        }

        roster.put(viewerUuid, viewer, new AlertFilter.Violator(cached.targetUuid()), target.playerName());
        viewer.sendMessage(platform.getMessageService().getComponent(
                MessagesKeys.FOCUS_ENABLED,
                Map.of("tg_player", target.playerName())
        ));
        return true;
    }

    private boolean resolveTesterEnabled(UUID uuid) {
        Boolean cached = cacheRepository.getAndRefresh(
                CacheKeys.testerToggle(uuid), CacheCodecs.BOOLEAN, TesterCommand.TESTER_TOGGLE_TTL);
        if (cached != null) return cached;

        boolean firstTimeDefault = true;
        cacheRepository.put(CacheKeys.testerToggle(uuid), firstTimeDefault,
                CacheCodecs.BOOLEAN, TesterCommand.TESTER_TOGGLE_TTL);
        return firstTimeDefault;
    }

    private StaffAlertPref resolveStaffAlertPref(UUID uuid) {
        StaffAlertPref cached = cacheRepository.getAndRefresh(
                CacheKeys.alertsPref(uuid), CacheCodecs.STAFF_ALERT_PREF, ALERTS_TOGGLE_TTL);
        if (cached != null) return cached;

        if (platform.getDatabaseRepository().isConnected()) {
            try {
                StaffAlertPref stored = platform.getDatabaseRepository().findStaffAlertPref(uuid);
                if (stored != null) {
                    cacheRepository.put(CacheKeys.alertsPref(uuid), stored,
                            CacheCodecs.STAFF_ALERT_PREF, ALERTS_TOGGLE_TTL);
                    return stored;
                }
            } catch (Exception ex) {
                platform.getLogger().warning(
                        "Failed to load staff alert preference for " + uuid + ": " + ex.getMessage());
            }
        }

        StaffAlertPref firstTimeDefault = new StaffAlertPref(true, false);
        cacheRepository.put(CacheKeys.alertsPref(uuid), firstTimeDefault,
                CacheCodecs.STAFF_ALERT_PREF, ALERTS_TOGGLE_TTL);
        if (platform.getDatabaseRepository().isConnected()) {
            try {
                platform.getDatabaseRepository().upsertStaffAlertPref(
                        uuid, firstTimeDefault.enabled(), firstTimeDefault.localOnly());
            } catch (Exception ex) {
                platform.getLogger().warning(
                        "Failed to persist default staff alert preference for " + uuid + ": " + ex.getMessage());
            }
        }
        return firstTimeDefault;
    }

    public void onPlayerDisconnect(final @NotNull User user) {
        UUID uuid = user.getUUID();
        if (uuid == null) return;

        clearExempt(uuid);
        exemptOnline.remove(uuid);
        platform.getAlertRepository().removeUser(uuid);

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        String name = user.getName();
        if (presence != null && name != null) {
            presence.onLocalPlayerQuit(uuid, name);
        }

        final TGPlayer player = players.remove(user);
        playersByUuid.remove(uuid);
        if (player == null) return;
        platform.getEventBus().getUserQuit().fire(player);
        player.onLogout();
    }

    public void removeUser(final @NotNull User user) {
        players.remove(user);
        UUID uuid = user.getUUID();
        if (uuid != null) {
            playersByUuid.remove(uuid);
        }
        clearExempt(uuid);
    }

    public @Nullable TGPlayer getPlayer(final @NotNull User user) {
        return players.get(user);
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

    public boolean shouldCheck(final @NotNull User user, final @Nullable PlatformPlayer platformPlayer) {
        final UUID uuid = user.getUUID();
        if (uuid == null) return false;

        if (isExempt(uuid)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (uuid.getMostSignificantBits() == 0L) {
            setExempt(uuid, true);
            return false;
        }

        if (platformPlayer != null && platformPlayer.hasPermission(BYPASS_PERMISSION)) {
            setExempt(uuid, true);
            return false;
        }

        return true;
    }

    @Override
    public @Nullable TGUser getUser(final @NotNull UUID uuid) {
        return playersByUuid.get(uuid);
    }

    public @Nullable TGPlayer getPlayer(final @NotNull UUID uuid) {
        return playersByUuid.get(uuid);
    }

    public @NotNull Collection<TGPlayer> getPlayers() {
        return players.values();
    }

    public @NotNull Map<UUID, ExemptPresence> exemptOnlinePresence() {
        return java.util.Collections.unmodifiableMap(exemptOnline);
    }

    public void backfillDatabaseProfiles() {
        for (TGPlayer player : getPlayers()) {
            if (player.getDatabasePlayerId() > 0) continue;
            if (!player.isHasLoggedIn()) continue;
            player.resolveDatabaseProfile();
        }
    }

    public record ExemptPresence(@NotNull String name, @Nullable UserProfile profile) {
    }
}
