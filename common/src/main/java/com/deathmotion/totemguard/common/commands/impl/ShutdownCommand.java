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

import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.util.Palette;
import com.deathmotion.totemguard.host.LoaderController;
import com.deathmotion.totemguard.host.TGPluginHost;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public final class ShutdownCommand extends AbstractCommand {

    private static final Duration CONFIRMATION_WINDOW = Duration.ofSeconds(30);

    private final ConcurrentMap<UUID, Instant> pendingConfirmations = new ConcurrentHashMap<>();

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("shutdown")
                        .permission(perm("shutdown"))
                        .optional(
                                "confirm",
                                StringParser.stringParser(),
                                SuggestionProvider.suggestingStrings("confirm")
                        )
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        Optional<String> argOpt = context.optional("confirm");
        boolean confirming = argOpt.map(s -> s.equalsIgnoreCase("confirm")).orElse(false);

        TGPlatform platform = TGPlatform.getInstance();
        boolean loaderManaged = platform.isManagedByLoader();

        UUID senderId = sender.getUniqueId();

        if (!confirming) {
            pendingConfirmations.put(senderId, Instant.now().plus(CONFIRMATION_WINDOW));
            sender.sendMessage(Component.text(
                    "/totemguard shutdown will STOP the anticheat. This server will be unprotected.",
                    Palette.WARN, TextDecoration.BOLD));
            if (loaderManaged) {
                sender.sendMessage(Component.empty()
                        .append(Component.text("  Loader detected. Bring TotemGuard back with ", Palette.CONNECTIVE))
                        .append(Component.text("/tgloader start", Palette.VALUE))
                        .append(Component.text(".", Palette.CONNECTIVE)));
            } else {
                sender.sendMessage(Component.text(
                        "  No loader is in play. TotemGuard CANNOT be restarted until the",
                        Palette.DANGER));
                sender.sendMessage(Component.text(
                        "  entire server process restarts.",
                        Palette.DANGER));
            }
            sender.sendMessage(Component.empty()
                    .append(Component.text("If you really want to stop TotemGuard, run ", Palette.LABEL))
                    .append(Component.text("/totemguard shutdown confirm", Palette.VALUE))
                    .append(Component.text(" within " + CONFIRMATION_WINDOW.toSeconds() + " seconds.", Palette.LABEL)));
            return;
        }

        Instant expiry = pendingConfirmations.remove(senderId);
        if (expiry == null || expiry.isBefore(Instant.now())) {
            sender.sendMessage(Component.text(
                    "No pending shutdown request to confirm. Run /totemguard shutdown first.",
                    Palette.DANGER));
            return;
        }

        if (loaderManaged) {
            shutdownViaLoader(sender, platform);
        } else {
            shutdownStandalone(sender, platform);
        }
    }

    private void shutdownViaLoader(Sender sender, TGPlatform platform) {
        TGPluginHost host = platform.getPluginHost();
        Optional<LoaderController> controllerOpt = host == null ? Optional.empty() : host.loaderController();
        if (controllerOpt.isEmpty()) {
            sender.sendMessage(Component.text(
                    "Loader-managed flag set but no loader controller present. Aborting.",
                    Palette.DANGER));
            return;
        }
        sender.sendMessage(Component.text("Stopping TotemGuard...", Palette.WARN));
        controllerOpt.get().stop(TGPluginShutdownEvent.Reason.LOADER_STOP).whenComplete((ok, err) -> {
            if (err != null) {
                platform.getLogger().log(Level.WARNING, "/totemguard shutdown (loader path) failed", err);
                // Sender may already be gone if commands torn down before this fires.
                try {
                    sender.sendMessage(Component.empty()
                            .append(Component.text("Shutdown failed: ", Palette.DANGER))
                            .append(Component.text(String.valueOf(err.getMessage()), Palette.VALUE_ON_DANGER)));
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private void shutdownStandalone(Sender sender, TGPlatform platform) {
        sender.sendMessage(Component.text(
                "Stopping TotemGuard. The server must be restarted to bring it back online.",
                Palette.WARN));
        platform.setShutdownReason(TGPluginShutdownEvent.Reason.OPERATOR_SHUTDOWN);
        platform.getScheduler().runMainThreadTask(platform::disablePlugin);
    }
}
