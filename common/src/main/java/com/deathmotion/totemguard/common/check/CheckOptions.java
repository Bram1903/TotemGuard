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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.CheckConfig;
import com.deathmotion.totemguard.common.punishment.PunishmentCommand;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
public class CheckOptions {

    /**
     * Mitigation is meaningful only for the inventory checks that actually act on it
     * inside CheckImpl#failInventory; surfacing the key elsewhere would be a no-op.
     */
    private static final Set<String> MITIGATION_CAPABLE = Set.of("InventoryA", "InventoryB", "InventoryC");

    private final String checkName;
    private final boolean enabled;
    private final boolean punishable;
    private final boolean mitigate;
    private final int maxViolations;
    private final List<PunishmentCommand> punishCommands;

    public CheckOptions(String checkName) {
        this.checkName = checkName;

        Optional<CheckConfig> typed = TGPlatform.getInstance()
                .getConfigRepository()
                .checks()
                .get(checkName);

        if (typed.isEmpty()) {
            if (!checkName.equals("Mod")) {
                TGPlatform.getInstance().getLogger().warning(
                        "Missing check configuration for '" + checkName + "' in checks.yml. Using fallback values."
                );
            }
            this.enabled = true;
            this.punishable = false;
            this.mitigate = false;
            this.maxViolations = 1;
            this.punishCommands = parsePunishCommands(List.of("%default_punishment%"));
            return;
        }

        CheckConfig cfg = typed.get();
        this.enabled = cfg.enabled();
        this.punishable = cfg.punishable();
        this.mitigate = MITIGATION_CAPABLE.contains(checkName) && cfg.mitigate();
        this.maxViolations = cfg.maxViolations();
        this.punishCommands = parsePunishCommands(cfg.punishmentCommands());
    }

    private static List<PunishmentCommand> parsePunishCommands(List<String> raw) {
        return raw.stream().map(PunishmentCommand::parse).toList();
    }
}
