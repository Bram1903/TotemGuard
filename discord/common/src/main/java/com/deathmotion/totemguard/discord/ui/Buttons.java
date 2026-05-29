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

import net.dv8tion.jda.api.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

public final class Buttons {
    private Buttons() {
    }

    public static @NotNull Button primary(@NotNull String id, @NotNull String label) {
        return Button.primary(id, label);
    }

    public static @NotNull Button secondary(@NotNull String id, @NotNull String label) {
        return Button.secondary(id, label);
    }

    public static @NotNull Button danger(@NotNull String id, @NotNull String label) {
        return Button.danger(id, label);
    }

    public static @NotNull Button link(@NotNull String url, @NotNull String label) {
        return Button.link(url, label);
    }

    public static @NotNull Button previous(@NotNull String id, boolean enabled) {
        return Button.secondary(id, "Previous").withDisabled(!enabled);
    }

    public static @NotNull Button next(@NotNull String id, boolean enabled) {
        return Button.secondary(id, "Next").withDisabled(!enabled);
    }
}
