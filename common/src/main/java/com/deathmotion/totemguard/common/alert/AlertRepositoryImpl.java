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

package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.api.alert.AlertRepository;
import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.PresenceListener;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.punishment.PunishmentRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncAlertMessagePacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncFocusAlertPacket;
import com.deathmotion.totemguard.common.util.MessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertRepositoryImpl implements AlertRepository, PresenceListener {

    private static final long CHAT_BUFFER_WINDOW_SECONDS = 1L;

    private final TGPlatform platform;
    private final MessageService messageService;
    private final PunishmentRepositoryImpl punishmentRepository;

    @Getter
    private final ConcurrentHashMap<UUID, PlatformPlayer> enabledAlerts = new ConcurrentHashMap<>();

    private final Set<UUID> localOnlyAlerts = ConcurrentHashMap.newKeySet();

    @Getter
    private final RealtimeAlertRoster realtimeRoster = new RealtimeAlertRoster();

    private final ConcurrentHashMap<UUID, PlayerChatBuffer> chatBuffers = new ConcurrentHashMap<>();

    public AlertRepositoryImpl() {
        this.platform = TGPlatform.getInstance();
        this.messageService = platform.getMessageService();
        this.punishmentRepository = platform.getPunishmentRepository();
    }

    @Override
    public boolean hasAlertsEnabled(UUID uuid) {
        return enabledAlerts.containsKey(uuid);
    }

    public boolean isLocalOnly(UUID uuid) {
        return localOnlyAlerts.contains(uuid);
    }

    @Override
    public boolean toggleAlerts(UUID uuid) {
        if (enabledAlerts.containsKey(uuid)) {
            PlatformPlayer user = enabledAlerts.remove(uuid);
            localOnlyAlerts.remove(uuid);
            if (user != null) {
                sendStatusMessage(user, MessagesKeys.ALERTS_DISABLED);
            }
            return false;
        }

        PlatformPlayer platformPlayer = TGPlatform.getInstance().getPlatformPlayerFactory().create(uuid);
        if (platformPlayer == null) return false;

        enabledAlerts.put(uuid, platformPlayer);
        sendStatusMessage(platformPlayer, localOnlyAlerts.contains(uuid)
                ? MessagesKeys.ALERTS_ENABLED_LOCAL_ONLY
                : MessagesKeys.ALERTS_ENABLED);
        return true;
    }

    public boolean toggleLocalOnly(UUID uuid) {
        PlatformPlayer player = enabledAlerts.get(uuid);
        if (player == null) {
            player = TGPlatform.getInstance().getPlatformPlayerFactory().create(uuid);
            if (player == null) return false;
            enabledAlerts.put(uuid, player);
        }

        boolean nowLocalOnly;
        if (localOnlyAlerts.remove(uuid)) {
            nowLocalOnly = false;
        } else {
            localOnlyAlerts.add(uuid);
            nowLocalOnly = true;
        }
        sendStatusMessage(player, nowLocalOnly
                ? MessagesKeys.ALERTS_LOCAL_ONLY_ENABLED
                : MessagesKeys.ALERTS_LOCAL_ONLY_DISABLED);
        return nowLocalOnly;
    }

    public void primeLocalOnly(UUID uuid, boolean localOnly) {
        if (localOnly) {
            localOnlyAlerts.add(uuid);
        } else {
            localOnlyAlerts.remove(uuid);
        }
    }

    public void removeUser(UUID uuid) {
        enabledAlerts.remove(uuid);
        localOnlyAlerts.remove(uuid);
        realtimeRoster.remove(uuid);

        PlayerChatBuffer playerChatBuffer = chatBuffers.remove(uuid);
        if (playerChatBuffer != null) {
            playerChatBuffer.clear();
        }
    }

    public void alert(CheckImpl check, int violations, @Nullable String debug) {
        UUID violatorUuid = check.player.getUuid();
        String violatorName = check.player.getName();
        Component realtimeMessage = AlertBuilder.build(check, violations, debug);
        realtimeRoster.deliver(violatorUuid, realtimeMessage);

        if (platform.getRedisRepository().isEnabled() && violatorName != null) {
            platform.getScheduler().runAsyncTask(() -> platform.getRedisRepository().publish(
                    Packets.SYNC_FOCUS_ALERT.packet(),
                    new SyncFocusAlertPacket.Payload(violatorUuid, violatorName, realtimeMessage)
            ));
        }

        bufferChatAlert(check, violations, debug);

        int keepalivePing = check.player.getPingData().getKeepAlivePing();
        int transactionPing = check.player.getPingData().getTransactionPing();
        platform.getDatabaseRepository().recordAlert(
                check.player.getDatabaseProfileId(),
                check.player.getDatabasePlayerId(),
                check.getName(),
                violations,
                debug,
                keepalivePing >= 0 ? keepalivePing : null,
                transactionPing >= 0 ? transactionPing : null,
                System.currentTimeMillis()
        );
        platform.getScheduler().runAsyncTask(() -> {
            platform.getDiscordWebhookService().sendAlert(check, violations, debug);
            punishmentRepository.punish(check, violations, debug);
        });
    }

    private void bufferChatAlert(CheckImpl check, int violations, @Nullable String debug) {
        chatBuffers.computeIfAbsent(
                check.player.getUuid(),
                ignored -> new PlayerChatBuffer(platform.getScheduler(), this::broadcastFlag, CHAT_BUFFER_WINDOW_SECONDS)
        ).buffer(check, violations, debug);
    }

    public void broadcast(String message) {
        broadcast(MessageUtil.formatMessage(message), true);
    }

    public void broadcast(Component message) {
        broadcast(message, true);
    }

    public void acceptRemoteAlert(SyncAlertMessagePacket.Payload payload) {
        deliverToBroadcastViewers(payload.violatorUuid(), payload.component(), true);
    }

    public void acceptRemoteFocusAlert(SyncFocusAlertPacket.Payload payload) {
        UUID violatorUuid = payload.violatorUuid();
        Component message = payload.component();
        for (AlertSubscription sub : realtimeRoster.matching(violatorUuid)) {
            if (!(sub.filter() instanceof AlertFilter.Violator)) continue;
            sub.viewer().sendMessage(message);
        }
    }

    private void sendStatusMessage(PlatformPlayer platformPlayer, ConfigKey<String> key) {
        platformPlayer.sendMessage(messageService.getComponent(key));
    }

    private void broadcastFlag(UUID violatorUuid, String violatorName, Component message) {
        deliverToBroadcastViewers(violatorUuid, message, false);

        if (!platform.getRedisRepository().shouldSend(MessagingTopic.ALERTS)) {
            return;
        }

        platform.getRedisRepository().publish(
                Packets.SYNC_ALERT_MESSAGE.packet(),
                SyncAlertMessagePacket.Payload.flag(violatorUuid, violatorName, message)
        );
    }

    private void broadcast(Component message, boolean syncRedis) {
        enabledAlerts.values().forEach(player -> player.sendMessage(message));

        if (!syncRedis || !platform.getRedisRepository().shouldSend(MessagingTopic.ALERTS)) {
            return;
        }

        platform.getRedisRepository().publish(
                Packets.SYNC_ALERT_MESSAGE.packet(),
                SyncAlertMessagePacket.Payload.broadcast(message)
        );
    }

    private void deliverToBroadcastViewers(@Nullable UUID violatorUuid, Component message, boolean remote) {
        for (Map.Entry<UUID, PlatformPlayer> entry : enabledAlerts.entrySet()) {
            UUID viewerUuid = entry.getKey();
            if (remote && localOnlyAlerts.contains(viewerUuid)) continue;
            AlertSubscription rt = realtimeRoster.get(viewerUuid);
            if (rt == null) {
                entry.getValue().sendMessage(message);
                continue;
            }
            if (rt.filter() instanceof AlertFilter.Violator) continue;
            if (rt.filter() instanceof AlertFilter.Self self
                    && violatorUuid != null && violatorUuid.equals(self.self())) continue;
            entry.getValue().sendMessage(message);
        }
    }

    @Override
    public void onPlayerOffline(UUID playerUuid, RemotePlayerEntry lastKnown) {
        for (AlertSubscription dropped : realtimeRoster.removeAllTargeting(playerUuid)) {
            String label = dropped.displayLabel() != null ? dropped.displayLabel() : lastKnown.playerName();
            dropped.viewer().sendMessage(messageService.getComponent(
                    MessagesKeys.FOCUS_TARGET_OFFLINE,
                    Map.of("tg_player", label)
            ));
            platform.getCacheRepository().remove(CacheKeys.focusTarget(dropped.viewerUuid()));
        }
    }
}
