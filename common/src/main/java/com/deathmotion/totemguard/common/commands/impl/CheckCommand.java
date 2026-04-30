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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.api.event.EventSubscription;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


/**
 * {@code /tg check <player> [duration]} — briefly clears the target's offhand
 * totem and watches whether they put a new one back within {@code duration}
 * milliseconds. A hit implies an auto-totem macro; a timeout is a pass.
 *
 * <p>Backend-only. {@link com.deathmotion.totemguard.common.commands.CommandManagerImpl}
 * skips registration on proxy platforms since we need real inventory access via
 * {@link PlatformPlayer}.</p>
 */
public final class CheckCommand extends AbstractCommand {

    private static final int DEFAULT_CHECK_DURATION_MS = 1500;
    private static final int MIN_DURATION_MS = 50;
    private static final int MAX_DURATION_MS = 5000;
    private static final long COOLDOWN_GRACE_MS = 1000L;

    private final TGPlatform platform;
    private final ConcurrentMap<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

    public CheckCommand() {
        this.platform = TGPlatform.getInstance();
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("check", Description.of("Force the target to re-totem and flag if they do"))
                        .required("tg_player", StringParser.stringParser(), TGPlayerSuggestionProvider.suggestionProvider())
                        .optional("duration", IntegerParser.integerParser(MIN_DURATION_MS, MAX_DURATION_MS))
                        .permission(perm("check"))
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String rawTarget = context.get("tg_player");
        int duration = context.<Integer>optional("duration").orElse(DEFAULT_CHECK_DURATION_MS);
        MessageService messages = platform.getMessageService();

        TGPlayer target = TGPlayerSuggestionProvider.findPlayer(rawTarget);
        if (target == null) {
            sender.sendMessage(messages.getComponent(
                    MessagesKeys.GENERAL_PLAYER_NOT_FOUND,
                    Map.of("tg_input", rawTarget)
            ));
            return;
        }

        PlatformPlayer platformPlayer = target.getPlatformPlayer();
        if (platformPlayer == null) {
            sender.sendMessage(messages.getComponent(MessagesKeys.CHECK_BACKEND_ONLY));
            return;
        }

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

        cooldownUntil.put(target.getUuid(), now + duration + COOLDOWN_GRACE_MS);
        runCheck(sender, target, platformPlayer, duration);
    }

    private void runCheck(Sender sender, TGPlayer target, PlatformPlayer platformPlayer, int durationMs) {
        target.setManualCheckActive(true);

        platformPlayer.beginManualCheck(
                handle -> {
                    AtomicBoolean concluded = new AtomicBoolean();
                    AtomicReference<EventSubscription> subscriptionRef = new AtomicReference<>();
                    long startedAt = System.currentTimeMillis();

                    Consumer<InventoryChangedEvent> listener = event -> {
                        if (event.getPlayer() != target) return;
                        if (event.getLastIssuer() != Issuer.CLIENT) return;

                        for (InventorySlot slot : event.getChangedSlots()) {
                            if (slot.getSlot() != InventoryConstants.SLOT_OFFHAND) continue;
                            if (!target.getInventory().isTotemInSlot(InventoryConstants.SLOT_OFFHAND)) continue;

                            long elapsed = System.currentTimeMillis() - startedAt;
                            if (concluded.compareAndSet(false, true)) {
                                conclude(sender, target, handle, subscriptionRef, true, elapsed, durationMs);
                            }
                            return;
                        }
                    };

                    EventRepositoryImpl events = platform.getEventRepository();
                    subscriptionRef.set(events.subscribeInternal(InventoryChangedEvent.class, listener));

                    platform.getScheduler().runAsyncTaskDelayed(() -> {
                        if (concluded.compareAndSet(false, true)) {
                            conclude(sender, target, handle, subscriptionRef, false, durationMs, durationMs);
                        }
                    }, durationMs, TimeUnit.MILLISECONDS);
                },
                () -> {
                    target.setManualCheckActive(false);
                    cooldownUntil.remove(target.getUuid());
                    sender.sendMessage(platform.getMessageService().getComponent(
                            MessagesKeys.CHECK_DAMAGE_FAILED, target));
                }
        );
    }

    private void conclude(Sender sender, TGPlayer target, ManualCheckHandle handle, AtomicReference<EventSubscription> subscriptionRef, boolean flagged, long elapsedMs, long windowMs) {
        EventSubscription subscription = subscriptionRef.getAndSet(null);
        if (subscription != null) {
            subscription.unsubscribe();
        }

        handle.restore();
        target.setManualCheckActive(false);

        MessageService messages = platform.getMessageService();
        if (flagged) {
            ManualTotemA check = target.getCheckManager().getManualCheck(ManualTotemA.class);
            if (check != null && check.isEnabled()) {
                check.handle(sender, elapsedMs, windowMs);
            }
            // The AlertRepository buffers the flag and fans it out to staff / Redis /
            // punishment — but the sender may not have alerts enabled, so still give
            // them an immediate confirmation.
            sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_FLAGGED,
                    target,
                    Map.of("tg_elapsed_ms", elapsedMs, "tg_window_ms", windowMs)
            ));
        } else {
            sendKey(sender, target, MessagesKeys.CHECK_PASSED);
        }
    }

    private void sendKey(Sender sender, TGPlayer target, ConfigKey<String> key) {
        sender.sendMessage(platform.getMessageService().getComponent(key, target));
    }

}
