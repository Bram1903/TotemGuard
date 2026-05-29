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

import com.deathmotion.totemguard.discord.bot.DiscordBot;
import com.deathmotion.totemguard.discord.command.impl.*;
import com.deathmotion.totemguard.discord.config.BotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public final class SlashCommandManager {
    private final DiscordBot bot;
    private final PermissionGate gate;
    private final String base;
    private final List<Long> guildIds;
    private final Map<String, SlashCommand> commands = new LinkedHashMap<>();
    private final Map<String, ComponentHandler> componentHandlers = new HashMap<>();
    private final SlashCommandData commandData;

    public SlashCommandManager(@NotNull DiscordBot bot) {
        this.bot = bot;
        BotConfig.CommandsConfig cfg = bot.config().commands();
        this.gate = new PermissionGate(cfg);
        this.base = cfg.base();
        this.guildIds = cfg.guildIds();

        for (SlashCommand command : defaults()) {
            if (cfg.isEnabled(command.name())) {
                commands.put(command.name(), command);
                if (command instanceof ComponentHandler handler) {
                    componentHandlers.put(handler.namespace(), handler);
                }
            }
        }

        SlashCommandData data = Commands.slash(base, "TotemGuard anticheat lookups and controls");
        for (SlashCommand command : commands.values()) {
            data.addSubcommands(command.data());
        }
        this.commandData = data;
    }

    private static List<SlashCommand> defaults() {
        return List.of(
                new HistoryCommand(),
                new StatusCommand(),
                new VersionCommand(),
                new StatsCommand(),
                new ReloadCommand(),
                new RestartCommand(),
                new UpdateCommand()
        );
    }

    private static @Nullable String[] split(@Nullable String customId) {
        if (customId == null || customId.isBlank()) return null;
        return customId.split(":");
    }

    public boolean usesGuildScope() {
        return !guildIds.isEmpty();
    }

    public void registerGlobal(@NotNull JDA jda) {
        if (commands.isEmpty()) return;
        jda.updateCommands().addCommands(commandData).queue(
                ok -> bot.platform().logger().info("Registered " + commands.size() + " Discord command(s) globally."),
                error -> bot.platform().logger().log(Level.WARNING, "Failed to register global Discord commands", error));
    }

    public void registerGuild(@NotNull Guild guild) {
        if (commands.isEmpty() || !guildIds.contains(guild.getIdLong())) return;
        guild.updateCommands().addCommands(commandData).queue(
                ok -> bot.platform().logger().info("Registered Discord commands in guild " + guild.getId() + "."),
                error -> bot.platform().logger().log(Level.WARNING,
                        "Failed to register Discord commands in guild " + guild.getId(), error));
    }

    public void dispatch(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals(base)) return;

        String subcommand = event.getSubcommandName();
        SlashCommand command = subcommand == null ? null : commands.get(subcommand);
        if (command == null) {
            event.reply("Unknown subcommand.").setEphemeral(true).queue();
            return;
        }

        SlashCommandContext context = new SlashCommandContext(event, bot);
        if (!gate.mayRun(command, context)) {
            event.reply("You do not have permission to run `/" + base + " " + command.name() + "`.")
                    .setEphemeral(true).queue();
            return;
        }

        event.deferReply(command.ephemeral()).queue(
                ok -> run(command, context),
                error -> bot.platform().logger().log(Level.WARNING,
                        "Failed to acknowledge command /" + base + " " + command.name(), error));
    }

    private void run(SlashCommand command, SlashCommandContext context) {
        try {
            command.handle(context);
        } catch (Exception e) {
            bot.errorReporter().report("/" + base + " " + command.name(), e);
            context.respondError("An internal error occurred while handling this command.");
        }
    }

    public void dispatchButton(@NotNull ButtonInteractionEvent event) {
        String[] parts = split(event.getComponentId());
        if (parts == null) return;
        ComponentHandler handler = componentHandlers.get(parts[0]);
        if (handler == null) return;
        try {
            handler.onButton(new ButtonContext(event, bot, Arrays.copyOfRange(parts, 1, parts.length)));
        } catch (Exception e) {
            bot.errorReporter().report("button " + event.getComponentId(), e);
        }
    }

    public void dispatchModal(@NotNull ModalInteractionEvent event) {
        String[] parts = split(event.getModalId());
        if (parts == null) return;
        ComponentHandler handler = componentHandlers.get(parts[0]);
        if (handler == null) return;
        try {
            handler.onModal(new ModalContext(event, bot, Arrays.copyOfRange(parts, 1, parts.length)));
        } catch (Exception e) {
            bot.errorReporter().report("modal " + event.getModalId(), e);
        }
    }
}
