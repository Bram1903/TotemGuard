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

package com.deathmotion.totemguard.bootstrap;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TotemGuardLoader implements PluginLoader {

    private static final TGVersion MIN_MOJANG_MAPPED_VERSION = TGVersion.fromString("1.20.5");

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        // Add Maven Central repository
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

        // Add all dependencies except CommandAPI ones
        List<Library> libraries = new ArrayList<>(EnumSet.allOf(Library.class));
        libraries.remove(Library.COMMAND_API);
        libraries.remove(Library.COMMAND_API_MOJANG_MAPPED);

        // Add correct CommandAPI variant based on server version
        TGVersion version = getServerVersion();
        libraries.add(version.isNewerThan(MIN_MOJANG_MAPPED_VERSION)
                ? Library.COMMAND_API_MOJANG_MAPPED
                : Library.COMMAND_API);

        for (Library lib : libraries) {
            resolver.addDependency(new Dependency(new DefaultArtifact(lib.getMavenDependency()), "runtime"));
        }

        try {
            classpathBuilder.addLibrary(resolver);
        } catch (LibraryLoadingException e) {
            throw new RuntimeException("Failed to load Maven dependencies", e);
        }
    }

    private TGVersion getServerVersion() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.json")) {
            if (inputStream == null) {
                throw new RuntimeException("version.json resource not found in the server JAR");
            }
            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
            String version = jsonObject.get("id").getAsString();
            return TGVersion.fromString(version);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read or parse version from server JAR: " + e.getMessage(), e);
        }
    }
}
