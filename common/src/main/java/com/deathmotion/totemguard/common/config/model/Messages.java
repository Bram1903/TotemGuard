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

package com.deathmotion.totemguard.common.config.model;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

@Getter
public class Messages {

    private final Component prefix;
    private final Component alertsEnabled;
    private final Component alertsDisabled;
    private final Component alertBrand;

    public Messages(ConfigurationNode config) throws SerializationException {
        this.prefix = config.node("prefix").get(Component.class);
        this.alertsEnabled = config.node("alerts-enabled").get(Component.class);
        this.alertsDisabled = config.node("alerts-disabled").get(Component.class);
        this.alertBrand = config.node("alert-brand").get(Component.class);
    }
}

