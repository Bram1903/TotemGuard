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
import com.deathmotion.totemguard.common.physics.trace.TickRecorder;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.Palette;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.List;

public final class PhysicsDumpCommand extends AbstractCommand {

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("physics")
                        .literal("dump")
                        .permission(perm("physics.dump"))
                        .handler(this::dumpSelf)
        );

        manager.command(
                base(manager)
                        .literal("physics")
                        .literal("dump")
                        .required("player", StringParser.stringParser(),
                                SuggestionProvider.blockingStrings((ctx, input) -> playerSuggestions(input.lastRemainingToken())))
                        .permission(perm("physics.dump"))
                        .handler(this::dumpNamed)
        );
    }

    private void dumpSelf(CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) return;
        dump(sender, sender.getTGPlayer());
    }

    private void dumpNamed(CommandContext<Sender> context) {
        String name = context.get("player");
        TGPlayer target = null;
        for (TGPlayer online : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
            if (online.getUser().getName().equalsIgnoreCase(name)) {
                target = online;
                break;
            }
        }
        dump(context.sender(), target);
    }

    private void dump(Sender sender, TGPlayer target) {
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", Palette.DANGER));
            return;
        }
        TickRecorder recorder = target.getPhysics().recorder();
        if (recorder == null || recorder.size() == 0) {
            sender.sendMessage(Component.text(
                    "No physics trace recorded. Set physics-engine.debug.level to summary or trace first.",
                    Palette.WARN));
            return;
        }
        boolean dumped = target.getPhysics().dumpTrace("command");
        sender.sendMessage(dumped
                ? Component.text("Physics trace for " + target.getUser().getName() + " dumped to the console.", Palette.SUCCESS)
                : Component.text("Dump rate limited, try again in a few seconds.", Palette.WARN));
    }

    private Iterable<String> playerSuggestions(String currentInput) {
        String normalized = currentInput.toLowerCase();
        List<String> suggestions = new ArrayList<>();
        for (TGPlayer online : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
            String name = online.getUser().getName();
            if (name != null && name.toLowerCase().startsWith(normalized)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}
