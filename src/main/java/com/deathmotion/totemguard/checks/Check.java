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
import com.deathmotion.totemguard.config.Messages;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.interfaces.AbstractCheckSettings;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicInteger;

// Class is heavily expired from https://github.com/Tecnio/antihaxerman/blob/master/src/main/java/me/tecnio/ahm/check/Check.java
@Getter
public class Check implements AbstractCheck {
    protected final TotemPlayer player;
    private final AtomicInteger violations = new AtomicInteger();
    protected Settings settings = TotemGuard.getInstance().getConfigManager().getSettings();
    protected Messages messages = TotemGuard.getInstance().getConfigManager().getMessages();
    protected Pair<TextColor, TextColor> color;
    protected AbstractCheckSettings checkSettings;
    private String checkName;
    private String description;
    private boolean experimental;

    public Check(TotemPlayer player) {
        this.player = player;
        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.checkSettings = TotemGuard.getInstance().getConfigManager().getChecks().getCheckSettings(checkName);
            this.description = checkData.description();
            this.experimental = checkData.experimental();

            this.color = getColors();
        }
    }

    public void reload() {
        this.settings = TotemGuard.getInstance().getConfigManager().getSettings();
        this.messages = TotemGuard.getInstance().getConfigManager().getMessages();

        final Class<?> checkClass = this.getClass();

        // Only put logic here if we are sure the check is annotated with @CheckData
        if (checkClass.isAnnotationPresent(CheckData.class)) {
            this.checkSettings = TotemGuard.getInstance().getConfigManager().getChecks().getCheckSettings(checkName);
        }

        this.color = getColors();
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
        if (settings.isApi()) {
            FlagEvent event = new FlagEvent(player, this);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        }

        return true;
    }

    @Override
    public int getViolations() {
        return violations.get();
    }

    @Override
    public int getMaxViolations() {
        return checkSettings.getMaxViolations();
    }

    private Pair<TextColor, TextColor> getColors() {
        MessengerService messengerService = TotemGuard.getInstance().getMessengerService();
        Messages.AlertFormat.CheckDetailsColor colorScheme = messages.getAlertFormat().getCheckDetailsColor();

        // Deserialize the color codes and extract the TextColor
        TextColor primaryColor = messengerService.format(colorScheme.getMain()).color();
        TextColor secondaryColor = messengerService.format(colorScheme.getSecondary()).color();

        return new Pair<>(primaryColor, secondaryColor);
    }
}
