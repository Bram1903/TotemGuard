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

package com.deathmotion.totemguard.loader.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class LoaderPalette {

    static final TextColor BRAND = TextColor.fromHexString("#FBAF00");
    static final TextColor VALUE = TextColor.fromHexString("#FEE067");
    static final TextColor LABEL = TextColor.fromHexString("#E7DEC4");
    static final TextColor CONNECTIVE = TextColor.fromHexString("#B8AC8F");
    static final TextColor CAPTION = TextColor.fromHexString("#6E6651");
    static final TextColor SUCCESS = TextColor.fromHexString("#6FB04A");
    static final TextColor DANGER = TextColor.fromHexString("#D4452C");
    static final TextColor DANGER_SOFT = TextColor.fromHexString("#F5A48F");

    static final Component PREFIX = Component.text()
            .append(Component.text("TGLoader", BRAND, TextDecoration.BOLD))
            .append(Component.space())
            .append(Component.text("»", CAPTION))
            .append(Component.space())
            .build();

    private LoaderPalette() {
    }
}
