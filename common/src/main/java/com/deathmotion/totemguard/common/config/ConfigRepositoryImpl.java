/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.config;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.serializer.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Paths;

public class ConfigRepositoryImpl {

    private final YamlConfigurationLoader loader;

    public ConfigRepositoryImpl() {
        this.loader = YamlConfigurationLoader.builder()
                .path(Paths.get(TGPlatform.getInstance().getPluginDirectory()))
                .defaultOptions(ConfigurationOptions.defaults().serializers(TypeSerializerCollection.defaults()
                        .childBuilder()
                        .registerExact(Component.class, new ComponentSerializer())
                        .build()))
                .build();
    }
}
