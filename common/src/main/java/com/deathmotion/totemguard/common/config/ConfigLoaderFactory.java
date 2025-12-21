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
import com.deathmotion.totemguard.common.config.codec.ComponentCodec;
import com.deathmotion.totemguard.common.config.codec.MessageFormat;
import com.deathmotion.totemguard.common.config.codec.impl.MiniMessageCodec;
import com.deathmotion.totemguard.common.config.codec.impl.NativeCodec;
import com.deathmotion.totemguard.common.config.serializer.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class ConfigLoaderFactory {

    private ConfigLoaderFactory() {
    }

    public static Path dataDir() {
        return Paths.get(TGPlatform.getInstance().getPluginDirectory());
    }

    public static Path resolve(final String fileName) {
        Objects.requireNonNull(fileName, "fileName");
        return dataDir().resolve(fileName);
    }

    public static ConfigurationOptions baseOptions() {
        return ConfigurationOptions.defaults();
    }

    public static ConfigurationOptions optionsWithMessageFormat(final MessageFormat format) {
        final ComponentCodec codec = switch (format) {
            case NATIVE -> new NativeCodec();
            case MINI_MESSAGE -> new MiniMessageCodec();
        };

        final TypeSerializerCollection serializers = TypeSerializerCollection.defaults()
                .childBuilder()
                .registerExact(Component.class, new ComponentSerializer(codec))
                .build();

        return baseOptions().serializers(serializers);
    }

    public static YamlConfigurationLoader yaml(final String fileName) {
        return yaml(fileName, baseOptions());
    }

    public static YamlConfigurationLoader yaml(final String fileName, final ConfigurationOptions options) {
        return YamlConfigurationLoader.builder()
                .path(resolve(fileName))
                .defaultOptions(options)
                .indent(4)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
    }

    public static YamlConfigurationLoader messagesYaml(final String fileName, final MessageFormat format) {
        return yaml(fileName, optionsWithMessageFormat(format));
    }
}
