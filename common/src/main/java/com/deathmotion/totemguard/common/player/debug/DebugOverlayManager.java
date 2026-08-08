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

package com.deathmotion.totemguard.common.player.debug;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.ActionBars;
import com.deathmotion.totemguard.common.util.Palette;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebugOverlayManager {

    private final TGPlayer player;
    private final Map<String, DebugOverlayProvider> providers = new LinkedHashMap<>();
    private final AtomicBoolean dispatchScheduled = new AtomicBoolean();

    @Getter
    private volatile @Nullable String activeOverlayKey;
    private volatile boolean dispatchDirty;

    public DebugOverlayManager(TGPlayer player) {
        this.player = player;
    }

    public void register(DebugOverlayProvider provider) {
        providers.put(normalize(provider.getKey()), provider);
    }

    public Set<String> registeredKeys() {
        return Set.copyOf(providers.keySet());
    }

    public @Nullable DebugOverlayProvider getProvider(String key) {
        return providers.get(normalize(key));
    }

    public boolean isEnabled(String key) {
        return activeOverlayKey != null && activeOverlayKey.equals(normalize(key));
    }

    public boolean toggle(String key) {
        String normalizedKey = normalize(key);

        if (normalizedKey.equals(activeOverlayKey)) {
            disable();
            return false;
        }

        if (!providers.containsKey(normalizedKey)) {
            throw new IllegalArgumentException("Unknown debug overlay: " + key);
        }

        activeOverlayKey = normalizedKey;
        refresh();
        return true;
    }

    public void disable() {
        activeOverlayKey = null;
        clear();
    }

    public void refresh() {
        if (activeOverlayKey == null) {
            return;
        }

        scheduleDispatch();
    }

    public void clear() {
        scheduleDispatch();
    }

    public @Nullable DebugOverlayProvider getActiveProvider() {
        return activeOverlayKey == null ? null : providers.get(activeOverlayKey);
    }

    private Component render(DebugOverlayFrame frame) {
        Component rendered = Component.empty();
        boolean firstLine = true;

        for (Component line : frame.lines()) {
            if (!firstLine) {
                rendered = rendered.append(Component.text(" || ", Palette.SEPARATOR));
            }

            rendered = rendered.append(line);
            firstLine = false;
        }

        return rendered;
    }

    private void scheduleDispatch() {
        this.dispatchDirty = true;

        if (!dispatchScheduled.compareAndSet(false, true)) {
            return;
        }

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            try {
                do {
                    this.dispatchDirty = false;
                    dispatchNow();
                } while (dispatchDirty);
            } finally {
                dispatchScheduled.set(false);

                if (dispatchDirty) {
                    scheduleDispatch();
                }
            }
        });
    }

    private void dispatchNow() {
        DebugOverlayProvider provider = getActiveProvider();
        Component message = provider == null ? Component.empty() : render(provider.buildFrame(player));
        ActionBars.send(player.getUser(), message);
    }

    private String normalize(String key) {
        return key.toLowerCase(Locale.ROOT);
    }
}
