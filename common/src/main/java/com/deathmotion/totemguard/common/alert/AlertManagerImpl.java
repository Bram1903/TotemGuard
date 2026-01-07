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

import com.deathmotion.totemguard.api.alert.AlertManager;
import com.deathmotion.totemguard.api.event.impl.TGAlertEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.reload.Reloadable;
import com.deathmotion.totemguard.common.util.MessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class AlertManagerImpl implements AlertManager, Reloadable {

    @Getter
    private final ConcurrentHashMap<UUID, PlatformUser> enabledAlerts = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<AlertKey, CompletableFuture<Void>> alertQueues = new ConcurrentHashMap<>();

    private final Executor asyncExecutor = command -> TGPlatform.getInstance().getScheduler().runAsyncTask(command);

    private ConfigRepositoryImpl configRepository;

    public AlertManagerImpl() {
        reload();
    }

    @Override
    public void reload() {
        configRepository = TGPlatform.getInstance().getConfigRepository();
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

    public void removeUser(TGUser user) {
        UUID uuid = user.getUuid();
        enabledAlerts.remove(uuid);
        alertQueues.keySet().removeIf(key -> key.playerUuid().equals(uuid));
    }

    public void alert(CheckImpl check, @Nullable String debug) {
        UUID playerUuid = check.player.getUuid();

        AlertKey key = new AlertKey(playerUuid, check.getName());

        alertQueues.compute(key, (k, tail) -> {
            CompletableFuture<Void> start = (tail == null)
                    ? CompletableFuture.completedFuture(null)
                    : tail;

            CompletableFuture<Void> next = start.thenRunAsync(() -> {
                String alertMessage = AlertBuilder.build(check, debug);

                TGAlertEvent event = new TGAlertEvent(
                        check.player,
                        check,
                        debug,
                        alertMessage
                );

                TGPlatform.getInstance()
                        .getEventRepository()
                        .post(event);

                if (!event.isCancelled()) {
                    broadcast(event.getAlertMessage());
                }
            }, asyncExecutor);

            next.whenComplete((v, t) -> alertQueues.remove(k, next));
            return next;
        });
    }

    private void broadcast(String message) {
        Component component = MessageUtil.formatMessage(message);
        enabledAlerts.values().forEach(player -> player.sendMessage(component));
    }

    private void sendToggleAlertMessage(PlatformUser platformUser, boolean enabled) {
        if (enabled) {
            platformUser.sendMessage(MessageUtil.formatMessage(configRepository.messagePrefix() + " &aAlerts enabled!"));
        } else {
            platformUser.sendMessage(MessageUtil.formatMessage(configRepository.messagePrefix() + " &cAlerts disabled!"));
        }
    }

    private record AlertKey(UUID playerUuid, String check) {
    }
}
