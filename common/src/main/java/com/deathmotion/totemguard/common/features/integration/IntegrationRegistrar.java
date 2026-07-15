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

package com.deathmotion.totemguard.common.features.integration;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.features.integration.impl.GrimIntegration;
import com.deathmotion.totemguard.common.features.integration.impl.LuckPermsIntegration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class IntegrationRegistrar {

    private static final String GRIM_PLUGIN_NAME = "GrimAC";
    private static final String LUCKPERMS_PLUGIN_NAME = "LuckPerms";
    private static final String LUCKPERMS_MOD_ID = "luckperms";

    private final List<Integration> integrations = new ArrayList<>();

    public IntegrationRegistrar() {
        registerIntegrations();
    }

    public void enableAll() {
        for (Integration integration : integrations) {
            try {
                integration.enable();
            } catch (Exception | LinkageError exception) {
                TGPlatform.getInstance().getLogger().severe(
                        "Failed to enable " + integration.getName() + " integration: " + exception.getMessage()
                );
            }
        }
    }

    public void disableAll() {
        for (Integration integration : integrations) {
            try {
                integration.disable();
            } catch (Exception | LinkageError exception) {
                TGPlatform.getInstance().getLogger().severe(
                        "Failed to disable " + integration.getName() + " integration: " + exception.getMessage()
                );
            }
        }

        integrations.clear();
    }

    private void registerIntegrations() {
        registerIfEnabled(this::createGrimIntegration, GRIM_PLUGIN_NAME);
        registerIfEnabled(this::createLuckPermsIntegration, LUCKPERMS_PLUGIN_NAME, LUCKPERMS_MOD_ID);
    }

    private void registerIfEnabled(Supplier<Integration> supplier, String... platformNames) {
        String detectedName = detect(platformNames);
        if (detectedName == null) {
            return;
        }

        try {
            integrations.add(supplier.get());
        } catch (LinkageError exception) {
            TGPlatform.getInstance().getLogger().severe(
                    "Failed to load " + detectedName + " integration: " + exception
            );
        }
    }

    private @Nullable String detect(String... platformNames) {
        for (String platformName : platformNames) {
            if (TGPlatform.getInstance().isPluginEnabled(platformName)) {
                return platformName;
            }
        }

        return null;
    }

    private Integration createGrimIntegration() {
        return new GrimIntegration();
    }

    private Integration createLuckPermsIntegration() {
        return new LuckPermsIntegration();
    }
}
