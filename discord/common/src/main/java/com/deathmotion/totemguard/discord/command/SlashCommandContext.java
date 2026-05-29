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

package com.deathmotion.totemguard.discord.command;

import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.discord.bot.DiscordBot;
import com.deathmotion.totemguard.discord.config.BotConfig;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public final class SlashCommandContext {
    private final SlashCommandInteractionEvent event;
    private final DiscordBot bot;

    public SlashCommandContext(@NotNull SlashCommandInteractionEvent event, @NotNull DiscordBot bot) {
        this.event = event;
        this.bot = bot;
    }

    public @NotNull SlashCommandInteractionEvent event() {
        return event;
    }

    public @NotNull DiscordBot bot() {
        return bot;
    }

    public @NotNull TotemGuardAPI api() {
        return bot.api();
    }

    public @NotNull BotConfig config() {
        return bot.config();
    }

    public @Nullable String optionString(@NotNull String name) {
        OptionMapping mapping = event.getOption(name);
        return mapping == null ? null : mapping.getAsString();
    }

    public void respond(@NotNull Container container) {
        event.getHook().editOriginalComponents(container).useComponentsV2()
                .queue(null, error -> bot.platform().logger()
                        .log(Level.WARNING, "Failed to send Discord reply", error));
    }

    public void respondError(@NotNull String message) {
        respond(Cv2.container(Colors.ERROR).heading("Something went wrong").text(message).build());
    }
}
