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

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class Cv2 {
    public static final int TEXT_LIMIT = 3900;

    private final List<ContainerChildComponent> children = new ArrayList<>();
    private final int accentColor;

    private Cv2(int accentColor) {
        this.accentColor = accentColor;
    }

    public static @NotNull Cv2 container(int accentColor) {
        return new Cv2(accentColor);
    }

    private static String truncate(String s) {
        return s.length() > TEXT_LIMIT ? s.substring(0, TEXT_LIMIT - 3) + "..." : s;
    }

    public @NotNull Cv2 text(@NotNull String markdown) {
        if (!markdown.isBlank()) {
            children.add(TextDisplay.of(truncate(markdown)));
        }
        return this;
    }

    public @NotNull Cv2 heading(@NotNull String heading) {
        return text("## " + heading);
    }

    public @NotNull Cv2 subtle(@NotNull String text) {
        return text("-# " + text);
    }

    public @NotNull Cv2 field(@NotNull String name, @NotNull String value) {
        return text("**" + name + "**\n" + value);
    }

    public @NotNull Cv2 section(@NotNull String markdown, @NotNull String thumbnailUrl) {
        children.add(Section.of(Thumbnail.fromUrl(thumbnailUrl), TextDisplay.of(truncate(markdown))));
        return this;
    }

    public @NotNull Cv2 codeBlock(@NotNull String content) {
        String body = content.length() > TEXT_LIMIT - 8 ? content.substring(0, TEXT_LIMIT - 8) + "..." : content;
        return text("```\n" + body + "\n```");
    }

    public @NotNull Cv2 divider() {
        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        return this;
    }

    public @NotNull Cv2 buttons(@NotNull Button @NotNull ... buttons) {
        if (buttons.length > 0) {
            children.add(ActionRow.of(List.of(buttons)));
        }
        return this;
    }

    public @NotNull Container build() {
        if (children.isEmpty()) {
            children.add(TextDisplay.of("​"));
        }
        return Container.of(children).withAccentColor(accentColor);
    }
}
