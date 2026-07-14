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
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.features.check.CheckService;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

public final class CheckCommand extends AbstractCommand {

    private static final int DEFAULT_CHECK_DURATION_MS = 1000;
    private static final int MIN_DURATION_MS = 50;
    private static final int MAX_DURATION_MS = 5000;

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

        CheckService service = TGPlatform.getInstance().getCheckService();
        if (service == null) return;
        service.execute(sender, rawTarget, duration);
    }
}
