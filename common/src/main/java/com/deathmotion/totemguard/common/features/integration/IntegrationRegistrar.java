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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class IntegrationRegistrar {

    private static final String GRIM_PLUGIN_NAME = "GrimAC";

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
        registerIfEnabled(GRIM_PLUGIN_NAME, this::createGrimIntegration);
    }

    private void registerIfEnabled(String pluginName, Supplier<Integration> supplier) {
        if (!TGPlatform.getInstance().isPluginEnabled(pluginName)) {
            return;
        }

        try {
            integrations.add(supplier.get());
        } catch (LinkageError exception) {
            TGPlatform.getInstance().getLogger().severe(
                    "Failed to load " + pluginName + " integration: " + exception
            );
        }
    }

    private Integration createGrimIntegration() {
        return new GrimIntegration();
    }
}
