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

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class SlashCommandListener extends ListenerAdapter {
    private final SlashCommandManager manager;
    private final Logger logger;

    public SlashCommandListener(@NotNull SlashCommandManager manager, @NotNull Logger logger) {
        this.manager = manager;
        this.logger = logger;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!manager.usesGuildScope() && event.getJDA().getShardInfo().getShardId() == 0) {
            manager.registerGlobal(event.getJDA());
        }
        logger.info("Discord shard " + event.getJDA().getShardInfo().getShardId() + " ready.");
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (manager.usesGuildScope()) {
            manager.registerGuild(event.getGuild());
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        manager.dispatch(event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        manager.dispatchButton(event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        manager.dispatchModal(event);
    }
}
