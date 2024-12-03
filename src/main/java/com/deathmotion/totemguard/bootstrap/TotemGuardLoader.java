package com.deathmotion.totemguard.bootstrap;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.jetbrains.annotations.NotNull;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

public class TotemGuardLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        // Initialize the MavenLibraryResolver
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        // Add repositories
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

        for (Library library : Library.values()) {
            resolver.addDependency(new Dependency(new DefaultArtifact(library.getMavenDependency()), "runtime"));
        }

        // Register resolver with the classpath builder
        try {
            classpathBuilder.addLibrary(resolver);
        } catch (LibraryLoadingException e) {
            throw new RuntimeException("Failed to load Maven dependencies", e);
        }
    }
}
