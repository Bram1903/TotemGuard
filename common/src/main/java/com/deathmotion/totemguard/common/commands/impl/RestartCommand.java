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

public final class RestartCommand extends AbstractCommand {

    private static final Duration CONFIRMATION_WINDOW = Duration.ofSeconds(30);

    private final ConcurrentMap<UUID, Instant> pendingConfirmations = new ConcurrentHashMap<>();

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("restart")
                        .permission(perm("restart"))
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
        TGPluginHost host = platform.getPluginHost();
        Optional<LoaderController> controllerOpt = host == null ? Optional.empty() : host.loaderController();
        if (controllerOpt.isEmpty()) {
            sender.sendMessage(Component.text(
                    "Restart unavailable: this server is not managed by the loader.",
                    Palette.DANGER));
            return;
        }
        LoaderController controller = controllerOpt.get();

        UUID senderId = sender.getUniqueId();

        if (!confirming) {
            pendingConfirmations.put(senderId, Instant.now().plus(CONFIRMATION_WINDOW));
            sender.sendMessage(Component.text(
                    "/totemguard restart is NOT the same as /totemguard reload.",
                    Palette.BRAND, TextDecoration.BOLD));
            sender.sendMessage(Component.empty()
                    .append(Component.text("  reload", Palette.VALUE))
                    .append(Component.text("  -> refreshes config files in place (cheap, ~50ms)", Palette.CONNECTIVE)));
            sender.sendMessage(Component.empty()
                    .append(Component.text("  restart", Palette.VALUE))
                    .append(Component.text(" -> tears down TotemGuard and reloads it from the loader cache", Palette.CONNECTIVE)));
            sender.sendMessage(Component.text(
                    "             (drops Redis/DB connections, rebuilds checks, alerts may pause briefly)",
                    Palette.CAPTION));
            sender.sendMessage(Component.empty()
                    .append(Component.text("If you meant to restart, run ", Palette.LABEL))
                    .append(Component.text("/totemguard restart confirm", Palette.VALUE))
                    .append(Component.text(" within " + CONFIRMATION_WINDOW.toSeconds() + " seconds.", Palette.LABEL)));
            return;
        }

        Instant expiry = pendingConfirmations.remove(senderId);
        if (expiry == null || expiry.isBefore(Instant.now())) {
            sender.sendMessage(Component.text(
                    "No pending restart request to confirm. Run /totemguard restart first.",
                    Palette.DANGER));
            return;
        }

        sender.sendMessage(Component.text("Restarting TotemGuard...", Palette.WARN));
        controller.restart().whenComplete((ok, err) -> {
            if (err != null) {
                platform.getLogger().log(Level.WARNING, "/totemguard restart failed", err);
                sender.sendMessage(Component.empty()
                        .append(Component.text("Restart failed: ", Palette.DANGER))
                        .append(Component.text(String.valueOf(err.getMessage()), Palette.VALUE_ON_DANGER)));
            } else {
                sender.sendMessage(Component.empty()
                        .append(Component.text("TotemGuard restarted on ", Palette.SUCCESS))
                        .append(Component.text(controller.info().loadedVersion(), Palette.VALUE))
                        .append(Component.text(".", Palette.SUCCESS)));
            }
        });
    }
}
