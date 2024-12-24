/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.checks;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.events.FlagEvent;
import com.deathmotion.totemguard.api.interfaces.AbstractCheck;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.interfaces.AbstractCheckSettings;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicInteger;

// Class is heavily expired from https://github.com/Tecnio/antihaxerman/blob/master/src/main/java/me/tecnio/ahm/check/Check.java
@Getter
public class Check implements AbstractCheck {
    protected final TotemPlayer player;
    protected Settings settings = TotemGuard.getInstance().getConfigManager().getSettings();
    protected AbstractCheckSettings checkSettings;

    private String checkName;
    private String description;
    private boolean experimental;

    private final AtomicInteger violations = new AtomicInteger();

    public Check(TotemPlayer player) {
        this.player = player;
        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.checkSettings = TotemGuard.getInstance().getConfigManager().getChecks().getCheckSettings(checkName);
            this.description = checkData.description();
            this.experimental = checkData.experimental();
        }
    }

    public void reload() {
        this.settings = TotemGuard.getInstance().getConfigManager().getSettings();
        this.checkSettings = TotemGuard.getInstance().getConfigManager().getChecks().getCheckSettings(checkName);
    }

    public void fail(Component details) {
        FoliaScheduler.getAsyncScheduler().runNow(TotemGuard.getInstance(), (O) -> {
            if (!shouldFail()) return;
            this.violations.incrementAndGet();

            TotemGuard.getInstance().getAlertManager().sendAlert(this, details);
            TotemGuard.getInstance().getPunishmentManager().punishPlayer(this, details);
        });
    }

    public boolean shouldFail() {
        if (settings.isAPI()) {
            FlagEvent event = new FlagEvent(player, this);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
        }

        return true;
    }

    @Override
    public int getViolations() {
        return violations.get();
    }

    @Override
    public int getMaxViolations() {
        return checkSettings.getPunishmentDelayInSeconds();
    }
}
