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

package com.deathmotion.totemguard.common.features.check;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.internal.InternalEventBus;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.network.ServerIdentity;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckRequestPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckResultPacket;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


public final class CheckService {

    static final long COOLDOWN_GRACE_MS = 1000L;
    static final long FAILSAFE_TIMEOUT_MS = 10_000L;

    final TGPlatform platform;
    final ConcurrentMap<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();
    final ConcurrentMap<UUID, ActiveCheck> active = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PendingRemoteRequest> pending = new ConcurrentHashMap<>();

    public CheckService() {
        this.platform = TGPlatform.getInstance();

        InternalEventBus internal = platform.getInternalEventBus();
        internal.getTotemActivated().register(platform, this::onTotemActivated);
        internal.getTotemReplenished().register(platform, this::onTotemReplenished);
        internal.getInventoryChanged().register(platform, this::onInventoryChanged);
        platform.getEventBus().getUserQuit().subscribe(platform, event -> onUserQuit(event.getUser().getUuid()));
    }

    public void execute(@NotNull Sender sender, @NotNull String rawTarget, int duration) {
        MessageService messages = platform.getMessageService();

        TGPlayer localTarget = TGPlayerSuggestionProvider.findPlayer(rawTarget);
        if (localTarget != null) {
            executeLocal(sender, localTarget, duration);
            return;
        }

        RemotePlayerEntry remote = TGPlayerSuggestionProvider.findNetworkPlayer(rawTarget);
        if (remote == null) {
            sender.sendMessage(messages.getComponent(
                    MessagesKeys.GENERAL_PLAYER_NOT_FOUND,
                    Map.of("tg_input", rawTarget)
            ));
            return;
        }

        if (!platform.getRedisRepository().isConnected()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_NO_REDIS));
            return;
        }

        dispatchRemote(sender, remote, duration);
    }

    private void executeLocal(Sender sender, TGPlayer target, int duration) {
        MessageService messages = platform.getMessageService();
        PlatformPlayer platformPlayer = target.getPlatformPlayer();

        if (target.isManualCheckActive()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_ALREADY_CHECKING, target));
            return;
        }

        long now = System.currentTimeMillis();
        Long until = cooldownUntil.get(target.getUuid());
        if (until != null && until > now) {
            sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_ON_COOLDOWN,
                    target,
                    Map.of("tg_remaining_ms", until - now)
            ));
            return;
        }

        if (!platformPlayer.isInSurvivalOrAdventure()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_WRONG_GAMEMODE, target));
            return;
        }

        if (platformPlayer.isInvulnerable()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_INVULNERABLE, target));
            return;
        }

        PacketInventory inventory = target.getInventory();
        if (!inventory.isTotemInSlot(InventoryConstants.SLOT_OFFHAND)) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_NO_TOTEM, target));
            return;
        }

        if (!inventory.hasBackupTotem()) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_NO_BACKUP_TOTEM, target));
            return;
        }

        cooldownUntil.put(target.getUuid(), now + duration + FAILSAFE_TIMEOUT_MS + COOLDOWN_GRACE_MS);
        String senderName = sender.getName();
        UUID senderUuid = sender.getUniqueId();
        runCheck(new LocalReporter(this, sender), target, platformPlayer, duration, senderName,
                () -> notifyLocalStaff(senderName, localServerName(), target.getName(), senderUuid));
    }

    private void dispatchRemote(Sender sender, RemotePlayerEntry remote, int duration) {
        MessageService messages = platform.getMessageService();
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_NO_REDIS));
            return;
        }

        ServerIdentity self = presence.identity();
        UUID requestId = UUID.randomUUID();
        UUID senderUuid = sender.getUniqueId() != null ? sender.getUniqueId() : Sender.CONSOLE_UUID;
        long ttlMs = FAILSAFE_TIMEOUT_MS + duration + COOLDOWN_GRACE_MS;
        long expiresAt = System.currentTimeMillis() + ttlMs;

        PendingRemoteRequest request = new PendingRemoteRequest(sender, remote.playerName(), duration);
        pending.put(requestId, request);
        request.timeoutTask = platform.getScheduler().runAsyncTaskDelayed(() -> {
            PendingRemoteRequest pendingRequest = pending.remove(requestId);
            if (pendingRequest == null) return;
            pendingRequest.sender.sendMessage(platform.getMessageService().getComponent(
                    MessagesKeys.CHECK_TIMEOUT,
                    Map.of("tg_player", pendingRequest.targetName)
            ));
        }, ttlMs, TimeUnit.MILLISECONDS);

        SyncCheckRequestPacket.Payload payload = new SyncCheckRequestPacket.Payload(
                requestId,
                self.instanceId(),
                senderUuid,
                sender.getName(),
                presence.getLocalServerName(),
                remote.serverInstanceId(),
                remote.playerUuid(),
                duration,
                expiresAt
        );
        platform.getRedisRepository().publishToInstance(
                remote.serverInstanceId(),
                Packets.SYNC_CHECK_REQUEST.<SyncCheckRequestPacket.Payload>packet(),
                payload
        );

        sender.sendMessage(messages.getComponent(
                MessagesKeys.CHECK_DISPATCHED,
                Map.of(
                        "tg_player", remote.playerName(),
                        "tg_server", remote.serverName()
                )
        ));
    }

    public void acceptRemoteCheckRequest(@NotNull SyncCheckRequestPacket.Payload payload) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;
        if (!presence.identity().instanceId().equals(payload.targetServerInstanceId())) return;
        if (System.currentTimeMillis() > payload.expiresAt()) return;

        platform.getScheduler().runAsyncTask(() -> handleRemoteRequest(payload));
    }

    private void handleRemoteRequest(@NotNull SyncCheckRequestPacket.Payload req) {
        TGPlayer target = platform.getPlayerRepository().getPlayer(req.targetUuid());
        if (target == null) {
            publishResult(req, "", SyncCheckResultPacket.STATUS_NOT_FOUND, 0L, 0L);
            return;
        }
        String targetName = target.getName() != null ? target.getName() : "";
        PlatformPlayer platformPlayer = target.getPlatformPlayer();
        if (platformPlayer == null) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_NOT_FOUND, 0L, 0L);
            return;
        }

        if (target.isManualCheckActive()) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_ALREADY_CHECKING, 0L, 0L);
            return;
        }

        long now = System.currentTimeMillis();
        Long until = cooldownUntil.get(target.getUuid());
        if (until != null && until > now) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_ON_COOLDOWN, 0L, until - now);
            return;
        }

        if (!platformPlayer.isInSurvivalOrAdventure()) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_WRONG_GAMEMODE, 0L, 0L);
            return;
        }

        if (platformPlayer.isInvulnerable()) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_INVULNERABLE, 0L, 0L);
            return;
        }

        if (!target.getInventory().isTotemInSlot(InventoryConstants.SLOT_OFFHAND)) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_NO_TOTEM, 0L, 0L);
            return;
        }

        if (!target.getInventory().hasBackupTotem()) {
            publishResult(req, targetName, SyncCheckResultPacket.STATUS_NO_BACKUP_TOTEM, 0L, 0L);
            return;
        }

        cooldownUntil.put(target.getUuid(), now + req.durationMs() + FAILSAFE_TIMEOUT_MS + COOLDOWN_GRACE_MS);
        runCheck(new RemoteReporter(this, req, targetName), target, platformPlayer, req.durationMs(), req.senderName(),
                () -> notifyLocalStaff(req.senderName(), req.senderServerName(), targetName, null));
    }

    public void acceptRemoteCheckResult(@NotNull SyncCheckResultPacket.Payload payload) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;
        if (!presence.identity().instanceId().equals(payload.senderInstanceId())) return;

        PendingRemoteRequest request = pending.remove(payload.requestId());
        if (request == null) return;
        if (request.timeoutTask != null) request.timeoutTask.cancel();

        deliverResultToSender(request.sender, payload);
    }

    private void deliverResultToSender(@NotNull Sender sender, @NotNull SyncCheckResultPacket.Payload p) {
        MessageService messages = platform.getMessageService();
        String name = p.targetName().isEmpty() ? "?" : p.targetName();
        switch (p.status()) {
            case SyncCheckResultPacket.STATUS_NOT_FOUND -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.GENERAL_PLAYER_NOT_FOUND,
                    Map.of("tg_input", name)));
            case SyncCheckResultPacket.STATUS_ALREADY_CHECKING -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_ALREADY_CHECKING,
                    Map.of("tg_player", name)));
            case SyncCheckResultPacket.STATUS_ON_COOLDOWN -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_ON_COOLDOWN,
                    Map.of("tg_player", name, "tg_remaining_ms", p.remainingMs())));
            case SyncCheckResultPacket.STATUS_WRONG_GAMEMODE -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_WRONG_GAMEMODE,
                    Map.of("tg_player", name)));
            case SyncCheckResultPacket.STATUS_INVULNERABLE -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_INVULNERABLE,
                    Map.of("tg_player", name)));
            case SyncCheckResultPacket.STATUS_NO_TOTEM -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_NO_TOTEM,
                    Map.of("tg_player", name)));
            case SyncCheckResultPacket.STATUS_NO_BACKUP_TOTEM -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_NO_BACKUP_TOTEM,
                    Map.of("tg_player", name)));
            case SyncCheckResultPacket.STATUS_DAMAGE_FAILED -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_DAMAGE_FAILED,
                    Map.of("tg_player", name)));
            case SyncCheckResultPacket.STATUS_FLAGGED -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_FLAGGED,
                    Map.of("tg_player", name,
                            "tg_elapsed_ms", p.elapsedMs(),
                            "tg_window_ms", (long) p.durationMs())));
            case SyncCheckResultPacket.STATUS_PASSED -> sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_PASSED,
                    Map.of("tg_player", name)));
            default -> { /* unknown status, drop silently */ }
        }
    }

    void publishResult(@NotNull SyncCheckRequestPacket.Payload req, @NotNull String targetName,
                       byte status, long elapsedMs, long remainingMs) {
        SyncCheckResultPacket.Payload payload = new SyncCheckResultPacket.Payload(
                req.requestId(),
                req.senderInstanceId(),
                req.senderUuid(),
                req.targetUuid(),
                targetName,
                status,
                elapsedMs,
                remainingMs,
                req.durationMs()
        );
        platform.getRedisRepository().publishToInstance(
                req.senderInstanceId(),
                Packets.SYNC_CHECK_RESULT.<SyncCheckResultPacket.Payload>packet(),
                payload
        );
    }

    private void runCheck(ResultReporter reporter, TGPlayer target, PlatformPlayer platformPlayer,
                          int durationMs, String staffName, Runnable onCheckStarted) {
        target.setManualCheckActive(true);

        ActiveCheck check = new ActiveCheck(this, reporter, target, durationMs, staffName);
        active.put(target.getUuid(), check);

        platformPlayer.beginManualCheck(
                handle -> {
                    check.installHandle(handle);
                    try {
                        onCheckStarted.run();
                    } catch (Exception ex) {
                        platform.getLogger().warning("Failed to fire check-started callback: " + ex.getMessage());
                    }
                },
                () -> {
                    active.remove(target.getUuid(), check);
                    target.setManualCheckActive(false);
                    cooldownUntil.remove(target.getUuid());
                    reporter.reportDamageFailed(target);
                }
        );
    }

    private void notifyLocalStaff(@NotNull String senderName, @NotNull String senderServerName,
                                  @NotNull String targetName, @Nullable UUID excludeUuid) {
        AlertRepositoryImpl alerts = platform.getAlertRepository();
        if (alerts == null) return;
        Map<UUID, ?> enabled = alerts.getEnabledAlerts();
        if (enabled.isEmpty()) return;

        String targetServerName = localServerName();
        boolean sameServer = senderServerName.equalsIgnoreCase(targetServerName);
        Component message = sameServer
                ? platform.getMessageService().getComponent(
                MessagesKeys.CHECK_STAFF_NOTICE_LOCAL,
                Map.of("tg_sender", senderName, "tg_player", targetName))
                : platform.getMessageService().getComponent(
                MessagesKeys.CHECK_STAFF_NOTICE,
                Map.of(
                        "tg_sender", senderName,
                        "tg_sender_server", senderServerName,
                        "tg_player", targetName,
                        "tg_target_server", targetServerName
                ));
        for (Map.Entry<UUID, ?> entry : enabled.entrySet()) {
            if (excludeUuid != null && excludeUuid.equals(entry.getKey())) continue;
            Object viewer = entry.getValue();
            if (viewer instanceof PlatformPlayer pp) {
                pp.sendMessage(message);
            }
        }
    }

    private @NotNull String localServerName() {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return "";
        return presence.getLocalServerName();
    }

    private void onTotemActivated(@NotNull TGPlayer player, long timestamp) {
        player.getCheckManager().onTotemActivated(timestamp);
        ActiveCheck check = active.get(player.getUuid());
        if (check == null) return;
        check.armDeadline();
    }

    private void onTotemReplenished(@NotNull TGPlayer player,
                                    long totemActivatedTimestamp,
                                    long totemReplenishedTimestamp,
                                    @Nullable Long totemPickupTimestamp) {
        player.getCheckManager().onTotemReplenished(totemActivatedTimestamp, totemReplenishedTimestamp, totemPickupTimestamp);
        ActiveCheck check = active.get(player.getUuid());
        if (check == null) return;
        check.observeReplenish(totemReplenishedTimestamp - totemActivatedTimestamp);
    }

    private void onInventoryChanged(@NotNull TGPlayer player,
                                    @Nullable CarriedItem updatedCarriedItem,
                                    @NotNull java.util.List<InventorySlot> changedSlots,
                                    @NotNull Issuer lastIssuer) {
        player.getCheckManager().onInventoryChanged(updatedCarriedItem, changedSlots, lastIssuer);
    }

    private void onUserQuit(@NotNull UUID playerUuid) {
        ActiveCheck check = active.remove(playerUuid);
        if (check == null) return;
        check.abort();
    }
}
