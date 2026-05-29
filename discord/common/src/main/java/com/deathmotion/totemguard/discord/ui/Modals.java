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

package com.deathmotion.totemguard.discord.ui;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Modals {
    private Modals() {
    }

    public static @NotNull Modal singleField(@NotNull String modalId, @NotNull String title,
                                             @NotNull String inputId, @NotNull String label,
                                             @Nullable String placeholder, boolean required) {
        TextInput.Builder input = TextInput.create(inputId, TextInputStyle.SHORT).setRequired(required);
        if (placeholder != null) input.setPlaceholder(placeholder);
        return Modal.create(modalId, title)
                .addComponents(Label.of(label, input.build()))
                .build();
    }
}
