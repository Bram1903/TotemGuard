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

import com.deathmotion.totemguard.api3.alert.AlertRepository;
import com.deathmotion.totemguard.api3.config.key.impl.MessagesKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.punishment.PunishmentRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.util.MessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class AlertRepositoryImpl implements AlertRepository {

    private final TGPlatform platform;
    private final MessageService messageService;
    private final PunishmentRepositoryImpl punishmentRepository;

    @Getter
    private final ConcurrentHashMap<UUID, PlatformUser> enabledAlerts = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> chatQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AlertKey, ChatBuffer> chatBuffers = new ConcurrentHashMap<>();
    private final Executor asyncExecutor = command -> TGPlatform.getInstance().getScheduler().runAsyncTask(command);

    public AlertRepositoryImpl() {
        this.platform = TGPlatform.getInstance();
        this.messageService = platform.getMessageService();
        this.punishmentRepository = platform.getPunishmentRepository();
    }

    @Override
    public boolean hasAlertsEnabled(UUID uuid) {
        return enabledAlerts.containsKey(uuid);
    }

    @Override
    public boolean toggleAlerts(UUID uuid) {
        if (enabledAlerts.containsKey(uuid)) {
            PlatformUser user = enabledAlerts.remove(uuid);
            if (user != null) {
                sendToggleAlertMessage(user, false);
            }
            return false;
        }

        PlatformUserCreation creation = TGPlatform.getInstance().getPlatformUserFactory().create(uuid);
        if (creation == null) return false;

        PlatformUser platformUser = creation.getPlatformUser();
        enabledAlerts.put(uuid, platformUser);
        sendToggleAlertMessage(platformUser, true);
        return true;
    }

    public void removeUser(UUID uuid) {
        enabledAlerts.remove(uuid);
        chatBuffers.keySet().removeIf(key -> key.playerUuid().equals(uuid));
        chatQueues.remove(uuid);
    }

    public void alert(CheckImpl check, int violations, @Nullable String debug) {
        bufferChatAlert(check, violations, debug);

        platform.getScheduler().runAsyncTask(() -> {
            punishmentRepository.punish(check, violations, debug);

            // TODO: Call punishment handler
            // TODO: Call Discord webhook handler
            // TODO: Call database handler
        });
    }

    private void bufferChatAlert(CheckImpl check, int violations, @Nullable String debug) {
        UUID playerUuid = check.player.getUuid();
        AlertKey key = new AlertKey(playerUuid, check.getName());

        chatBuffers.compute(key, (k, buf) -> {
            if (buf == null) {
                ChatBuffer created = new ChatBuffer(check);
                created.update(violations, debug);

                scheduleFlush(k);

                return created;
            }

            buf.update(violations, debug);
            return buf;
        });
    }

    private void scheduleFlush(AlertKey key) {
        TGPlatform.getInstance().getScheduler().runAsyncTaskDelayed(() -> flushChat(key), 1, TimeUnit.SECONDS);
    }

    private void flushChat(AlertKey key) {
        ChatBuffer buf = chatBuffers.remove(key);
        if (buf == null) {
            return;
        }

        int finalVl = buf.getViolations();
        String finalDebug = buf.getDebug();
        CheckImpl check = buf.getCheck();

        Component alertMessage = AlertBuilder.build(check, finalVl, finalDebug);
        enqueueChatSend(key.playerUuid(), alertMessage);
    }

    private void enqueueChatSend(UUID playerUuid, Component message) {
        chatQueues.compute(playerUuid, (uuid, tail) -> {
            CompletableFuture<Void> start = (tail == null)
                    ? CompletableFuture.completedFuture(null)
                    : tail;

            CompletableFuture<Void> next = start.thenRunAsync(() -> broadcast(message), asyncExecutor);

            next.whenComplete((v, t) -> chatQueues.remove(uuid, next));
            return next;
        });
    }

    public void broadcast(String message) {
        broadcast(MessageUtil.formatMessage(message), true);
    }

    public void broadcast(Component message) {
        broadcast(message, true);
    }

    public void broadcastRawComponent(Component message) {
        broadcast(message, false);
    }

    private void sendToggleAlertMessage(PlatformUser platformUser, boolean enabled) {
        platformUser.sendMessage(
                messageService.getComponent(
                        enabled ? MessagesKeys.ALERTS_ENABLED : MessagesKeys.ALERTS_DISABLED
                )
        );
    }

    private void broadcast(Component message, boolean syncRedis) {
        enabledAlerts.values().forEach(player -> player.sendMessage(message));

        if (!syncRedis || !platform.getRedisRepository().shouldSendAlerts()) {
            return;
        }

        platform.getRedisRepository().publish(Packets.SYNC_ALERT_MESSAGE.packet(), message);
    }
}
