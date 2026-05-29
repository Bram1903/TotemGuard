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
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class ButtonContext {
    private final ButtonInteractionEvent event;
    private final DiscordBot bot;
    private final String[] args;

    public ButtonContext(@NotNull ButtonInteractionEvent event, @NotNull DiscordBot bot, @NotNull String[] args) {
        this.event = event;
        this.bot = bot;
        this.args = args;
    }

    public @NotNull ButtonInteractionEvent event() {
        return event;
    }

    public @NotNull DiscordBot bot() {
        return bot;
    }

    public @NotNull TotemGuardAPI api() {
        return bot.api();
    }

    public int argCount() {
        return args.length;
    }

    public @NotNull String arg(int index) {
        return args[index];
    }

    public @NotNull String argsFrom(int index) {
        return String.join(":", java.util.Arrays.copyOfRange(args, index, args.length));
    }

    public void edit(@NotNull Container container) {
        event.deferEdit().queue(
                ok -> event.getHook().editOriginalComponents(container).useComponentsV2().queue(null, this::warn),
                this::warn);
    }

    public void openModal(@NotNull Modal modal) {
        event.replyModal(modal).queue(null, this::warn);
    }

    public void error(@NotNull String message) {
        edit(Cv2.container(Colors.ERROR).heading("Something went wrong").text(message).build());
    }

    private void warn(Throwable error) {
        bot.platform().logger().log(Level.FINE, "Discord button interaction failed", error);
    }
}
