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

package com.deathmotion.totemguard.discord.command.impl;

import com.deathmotion.totemguard.api.history.AlertEntry;
import com.deathmotion.totemguard.api.history.HistoryPage;
import com.deathmotion.totemguard.api.history.HistoryView;
import com.deathmotion.totemguard.api.result.Result;
import com.deathmotion.totemguard.discord.bot.DiscordBot;
import com.deathmotion.totemguard.discord.command.*;
import com.deathmotion.totemguard.discord.ui.*;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class HistoryCommand implements SlashCommand, ComponentHandler {
    private static final int LABEL_ID_LIMIT = 50;

    private static @Nullable UUID resolve(String input, DiscordBot bot) {
        String trimmed = input.trim();
        if (trimmed.indexOf('-') >= 0) {
            try {
                return UUID.fromString(trimmed);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return bot.platform().resolveUuid(trimmed).orElse(null);
    }

    private static String message(@Nullable Result<HistoryPage<AlertEntry>> result) {
        if (result != null && result.message() != null) return result.message();
        return "History is currently unavailable.";
    }

    private static String sanitize(String input) {
        return input.replace("`", "").replace("*", "").replace("_", "");
    }

    private static String sanitizeId(String label) {
        String cleaned = label.replace(":", "").trim();
        return cleaned.length() > LABEL_ID_LIMIT ? cleaned.substring(0, LABEL_ID_LIMIT) : cleaned;
    }

    @Override
    public @NotNull String name() {
        return "history";
    }

    @Override
    public @NotNull String namespace() {
        return "hist";
    }

    @Override
    public @NotNull SubcommandData data() {
        return new SubcommandData("history", "Look up a player's alert and punishment history.")
                .addOptions(new OptionData(OptionType.STRING, "player", "Player name or UUID", true));
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public void handle(@NotNull SlashCommandContext context) {
        String input = context.optionString("player");
        if (input == null || input.isBlank()) {
            context.respondError("Provide a player name or UUID.");
            return;
        }

        UUID uuid = resolve(input, context.bot());
        if (uuid == null) {
            context.respondError("Couldn't find a player matching `" + sanitize(input) + "`. Try a full UUID.");
            return;
        }

        render(context.bot(), input.trim(), uuid, 0, context::respond, context::respondError);
    }

    @Override
    public void onButton(@NotNull ButtonContext context) {
        if (context.argCount() == 0) return;

        if (context.arg(0).equals("lookup")) {
            context.openModal(Modals.singleField("hist:lookup", "Look up player history",
                    "query", "Player", "Name or UUID", true));
            return;
        }

        if (context.arg(0).equals("p") && context.argCount() >= 3) {
            UUID uuid;
            int page;
            try {
                uuid = UUID.fromString(context.arg(1));
                page = Math.max(0, Integer.parseInt(context.arg(2)));
            } catch (RuntimeException e) {
                context.error("That button is no longer valid.");
                return;
            }
            String label = context.argCount() > 3 ? context.argsFrom(3) : context.arg(1);
            render(context.bot(), label, uuid, page, context::edit, context::error);
        }
    }

    @Override
    public void onModal(@NotNull ModalContext context) {
        if (context.argCount() == 0 || !context.arg(0).equals("lookup")) return;

        String query = context.value("query");
        if (query == null || query.isBlank()) {
            context.error("Provide a player name or UUID.");
            return;
        }

        UUID uuid = resolve(query, context.bot());
        if (uuid == null) {
            context.error("Couldn't find a player matching `" + sanitize(query) + "`. Try a full UUID.");
            return;
        }

        render(context.bot(), query.trim(), uuid, 0, context::edit, context::error);
    }

    private void render(DiscordBot bot, String label, UUID uuid, int page,
                        Consumer<Container> onCard, Consumer<String> onError) {
        HistoryView view = bot.api().getHistoryRepository().of(uuid);
        view.alerts(page).thenAcceptBoth(view.punishmentCount(), (alertsResult, punishResult) -> {
            if (alertsResult == null || !alertsResult.ok() || alertsResult.value() == null) {
                onError.accept(message(alertsResult));
                return;
            }
            HistoryPage<AlertEntry> alerts = alertsResult.value();
            int punishments = punishResult != null && punishResult.ok() && punishResult.value() != null
                    ? punishResult.value() : -1;
            onCard.accept(card(label, uuid, page, alerts, punishments));
        }).exceptionally(error -> {
            bot.platform().logger().log(Level.WARNING, "Discord history query failed", error);
            onError.accept("Lookup failed.");
            return null;
        });
    }

    private Container card(String label, UUID uuid, int page, HistoryPage<AlertEntry> alerts, int punishments) {
        Cv2 card = Cv2.container(Colors.BRAND)
                .heading("History: " + sanitize(label))
                .text("**Total alerts:** `" + alerts.totalEntries() + "`"
                        + (punishments >= 0 ? "  •  **Punishments:** `" + punishments + "`" : ""))
                .subtle("UUID `" + uuid + "`")
                .divider();

        if (alerts.isEmpty()) {
            card.text("No alerts on record.");
        } else {
            StringBuilder body = new StringBuilder();
            for (AlertEntry entry : alerts.entries()) {
                body.append("• **").append(entry.checkName()).append("** · `")
                        .append(entry.serverName()).append("` · ")
                        .append(Format.relative(entry.createdAt())).append('\n');
            }
            card.text(body.toString().stripTrailing());
            if (alerts.totalPages() > 1) {
                card.subtle("Page " + (page + 1) + " of " + alerts.totalPages());
            }
        }

        String labelId = sanitizeId(label);
        card.buttons(
                Buttons.previous("hist:p:" + uuid + ":" + (page - 1) + ":" + labelId, page > 0),
                Buttons.next("hist:p:" + uuid + ":" + (page + 1) + ":" + labelId, page < alerts.totalPages() - 1),
                Buttons.primary("hist:lookup", "Look up player"));

        return card.build();
    }
}
