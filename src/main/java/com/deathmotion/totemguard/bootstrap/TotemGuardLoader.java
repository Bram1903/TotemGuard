package com.deathmotion.totemguard.bootstrap;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TotemGuardLoader implements PluginLoader {

    // Use JitPack for the ConfigLib dependency
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
        var baseLibs = new ArrayList<>(EnumSet.allOf(Library.class));
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
}
