/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.models.checks;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.List;

@Getter
@Setter
public class CheckDetails {
    private String checkName;
    private String checkDescription;
    private Component alert;
    private Component details;
    private int violations;
    private int tps;
    private int ping;
    private String gamemode;
    private boolean experimental;
    private boolean enabled;
    private boolean punishable;
    private int punishmentDelay;
    private int maxViolations;
    private List<String> punishmentCommands;
}
