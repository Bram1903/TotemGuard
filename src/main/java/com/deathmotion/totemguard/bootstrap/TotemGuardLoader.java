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
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class TotemGuardLoader implements PluginLoader {

    private static final TGVersion MIN_MOJANG_MAPPED_VERSION = TGVersion.fromString("1.20.5");

    // Use JitPack as the backup repository
    private static final RemoteRepository BACKUP_REPO = new RemoteRepository.Builder(
            "jitpack", "default",
            "https://jitpack.io"
    ).build();

    private static final List<RemoteRepository> REPOSITORIES = List.of(
            new RemoteRepository.Builder(
                    "google-maven-central", "default",
                    "https://maven-central.storage-download.googleapis.com/maven2"
            ).build(),
            BACKUP_REPO
    );

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        TGVersion serverVersion = getServerVersion();

        var commandApi = serverVersion.isNewerThan(MIN_MOJANG_MAPPED_VERSION)
                ? Library.COMMAND_API_MOJANG_MAPPED
                : Library.COMMAND_API;

        var baseLibs = EnumSet.allOf(Library.class).stream()
                .filter(lib -> lib != Library.COMMAND_API && lib != Library.COMMAND_API_MOJANG_MAPPED)
                .collect(Collectors.toList());
        baseLibs.add(commandApi);

        var configLib = Library.CONFIGLIB;
        baseLibs.remove(configLib);

        MavenLibraryResolver baseResolver = new MavenLibraryResolver();
        REPOSITORIES.forEach(baseResolver::addRepository);
        baseLibs.forEach(lib -> baseResolver.addDependency(
                new Dependency(new DefaultArtifact(lib.getMavenDependency()), "runtime")
        ));

        try {
            classpathBuilder.addLibrary(baseResolver);
        } catch (LibraryLoadingException e) {
            throw new RuntimeException("Failed to load base dependencies", e);
        }

        MavenLibraryResolver configResolver = new MavenLibraryResolver();
        configResolver.addRepository(BACKUP_REPO);
        configResolver.addDependency(
                new Dependency(new DefaultArtifact(configLib.getMavenDependency()), "runtime")
        );

        try {
            classpathBuilder.addLibrary(configResolver);
        } catch (LibraryLoadingException e) {
            throw new RuntimeException("Failed to load ConfigLib from JitPack repository", e);
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
