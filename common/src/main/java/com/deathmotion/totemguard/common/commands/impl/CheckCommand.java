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
import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.internal.impl.TotemActivatedEvent;
import com.deathmotion.totemguard.common.event.internal.impl.TotemReplenishedEvent;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
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


public final class CheckCommand extends AbstractCommand {

    private static final int DEFAULT_CHECK_DURATION_MS = 1000;
    private static final int MIN_DURATION_MS = 50;
    private static final int MAX_DURATION_MS = 5000;
    private static final long COOLDOWN_GRACE_MS = 1000L;
    private static final long FAILSAFE_TIMEOUT_MS = 10_000L;

    private final TGPlatform platform;
    private final ConcurrentMap<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ActiveCheck> active = new ConcurrentHashMap<>();

    public CheckCommand() {
        this.platform = TGPlatform.getInstance();

        EventRepositoryImpl events = platform.getEventRepository();
        events.subscribeInternal(TotemActivatedEvent.class, this::onTotemActivated);
        events.subscribeInternal(TotemReplenishedEvent.class, this::onTotemReplenished);
        events.subscribe(TGUserQuitEvent.class, this::onUserQuit);
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("check", Description.of("Force the target to re-totem and flag if they do"))
                        .required("tg_player", StringParser.stringParser(), TGPlayerSuggestionProvider.suggestionProviderExcludingSelf())
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

        cooldownUntil.put(target.getUuid(), now + duration + FAILSAFE_TIMEOUT_MS + COOLDOWN_GRACE_MS);
        runCheck(sender, target, platformPlayer, duration);
    }

    private void runCheck(Sender sender, TGPlayer target, PlatformPlayer platformPlayer, int durationMs) {
        target.setManualCheckActive(true);

        ActiveCheck check = new ActiveCheck(sender, target, durationMs);
        active.put(target.getUuid(), check);

        platformPlayer.beginManualCheck(
                check::installHandle,
                () -> {
                    active.remove(target.getUuid(), check);
                    target.setManualCheckActive(false);
                    cooldownUntil.remove(target.getUuid());
                    sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.CHECK_DAMAGE_FAILED, target));
                }
        );
    }

    private void onTotemActivated(TotemActivatedEvent event) {
        ActiveCheck check = active.get(event.getPlayer().getUuid());
        if (check == null) return;
        check.armDeadline();
    }

    private void onTotemReplenished(TotemReplenishedEvent event) {
        ActiveCheck check = active.get(event.getPlayer().getUuid());
        if (check == null) return;
        check.observeReplenish(event.getDelayMillis());
    }

    private void onUserQuit(TGUserQuitEvent event) {
        ActiveCheck check = active.remove(event.getUser().getUuid());
        if (check == null) return;
        check.abort();
    }

    private void sendVerdict(ActiveCheck check, boolean flagged, long elapsedMs) {
        MessageService messages = platform.getMessageService();
        if (flagged) {
            ManualTotemA detector = check.target.getCheckManager().getManualCheck(ManualTotemA.class);
            if (detector != null && detector.isEnabled()) {
                detector.handle(check.sender, elapsedMs, check.durationMs);
            }
            check.sender.sendMessage(messages.getComponent(
                    MessagesKeys.CHECK_FLAGGED,
                    check.target,
                    Map.of("tg_elapsed_ms", elapsedMs, "tg_window_ms", (long) check.durationMs)
            ));
        } else {
            sendKey(check.sender, check.target, MessagesKeys.CHECK_PASSED);
        }
    }

    private void sendKey(Sender sender, TGPlayer target, ConfigKey<String> key) {
        sender.sendMessage(platform.getMessageService().getComponent(key, target));
    }

    private final class ActiveCheck {
        private final Sender sender;
        private final TGPlayer target;
        private final int durationMs;
        private final AtomicBoolean concluded = new AtomicBoolean();
        private final AtomicBoolean deadlineArmed = new AtomicBoolean();
        private final AtomicReference<ManualCheckHandle> handleRef = new AtomicReference<>();

        ActiveCheck(Sender sender, TGPlayer target, int durationMs) {
            this.sender = sender;
            this.target = target;
            this.durationMs = durationMs;
        }

        void installHandle(ManualCheckHandle handle) {
            handleRef.set(handle);
            if (concluded.get()) {
                ManualCheckHandle pending = handleRef.getAndSet(null);
                if (pending != null) pending.restore();
                return;
            }

            platform.getScheduler().runAsyncTaskDelayed(() -> {
                if (deadlineArmed.get()) return;
                resolve(false, 0L);
            }, FAILSAFE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        void armDeadline() {
            if (!deadlineArmed.compareAndSet(false, true)) return;
            platform.getScheduler().runAsyncTaskDelayed(
                    () -> resolve(false, durationMs),
                    durationMs,
                    TimeUnit.MILLISECONDS
            );
        }

        void observeReplenish(long elapsedMs) {
            if (elapsedMs < 0L) return;
            if (!target.getInventory().isTotemInSlot(InventoryConstants.SLOT_OFFHAND)) return;
            resolve(elapsedMs <= durationMs, elapsedMs);
        }

        private void resolve(boolean flagged, long elapsedMs) {
            if (!concluded.compareAndSet(false, true)) return;

            cleanup();
            cooldownUntil.put(target.getUuid(), System.currentTimeMillis() + COOLDOWN_GRACE_MS);
            sendVerdict(this, flagged, elapsedMs);
        }

        void abort() {
            if (!concluded.compareAndSet(false, true)) return;
            cleanup();
            cooldownUntil.remove(target.getUuid());
        }

        private void cleanup() {
            active.remove(target.getUuid(), this);
            ManualCheckHandle handle = handleRef.getAndSet(null);
            if (handle != null) handle.restore();
            target.setManualCheckActive(false);
        }
    }
}
