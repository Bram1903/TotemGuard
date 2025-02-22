package com.deathmotion.totemguard.bootstrap;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.deathmotion.totemguard.TotemGuard;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LibraryLoader {
    public void loadLibraries(final @NotNull LibraryManager libraryManager) {
        libraryManager.addMavenCentral();
        libraryManager.addRepository("https://s01.oss.sonatype.org/content/repositories/snapshots");
        libraryManager.loadLibraries(
                Library.builder()
                        .groupId("de{}exlll")
                        .artifactId("configlib-yaml")
                        .version("4.5.0")
                        .relocate("de{}exlll{}configlib", "com{}deathmotion{}totemguard{}libs{}configlib")
                        .resolveTransitiveDependencies(true)
                        .build(),
                Library.builder()
                        .groupId("io{}lettuce")
                        .artifactId("lettuce-core")
                        .version("6.5.1.RELEASE")
                        .relocate("io{}lettuce{}core", "com{}deathmotion{}totemguard{}libs{}lettuce")
                        .resolveTransitiveDependencies(false)
                        .build(),
                Library.builder()
                        .groupId("net{}jodah")
                        .artifactId("expiringmap")
                        .version("0.5.11")
                        .relocate("net{}jodah{}expiringmap", "com{}deathmotion{}totemguard{}libs{}expiringmap")
                        .resolveTransitiveDependencies(false)
                        .build(),
                Library.builder()
                        .groupId("io{}ebean")
                        .artifactId("ebean")
                        .version("15.9.0")
                        .relocate("io{}ebean", "com{}deathmotion{}totemguard{}libs{}ebean")
                        .resolveTransitiveDependencies(true)
                        .build(),
                Library.builder()
                        .groupId("com{}h2database")
                        .artifactId("h2")
                        .version("2.3.232")
                        .relocate("org{}h2", "com{}deathmotion{}totemguard{}libs{}h2")
                        .resolveTransitiveDependencies(false)
                        .build(),
                Library.builder()
                        .groupId("mysql")
                        .artifactId("mysql-connector-java")
                        .version("8.0.30")
                        .relocate("com{}mysql{}cj", "com{}deathmotion{}totemguard{}libs{}mysql")
                        .resolveTransitiveDependencies(false)
                        .build()
        );

        // Check if running on PaperMC and if the server version is newer than 1.20.4
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_4) && TotemGuard.getInstance().isPaper()) {
            libraryManager.loadLibraries(
                    Library.builder()
                            .groupId("dev{}jorel")
                            .artifactId("commandapi-bukkit-shade-mojang-mapped")
                            .version("9.7.1-SNAPSHOT")
                            .relocate("dev{}jorel{}commandapi", "com{}deathmotion{}totemguard{}libs{}commandapi")
                            .resolveTransitiveDependencies(true)
                            .build()
            );
        } else {
            libraryManager.loadLibraries(
                    Library.builder()
                            .groupId("dev{}jorel")
                            .artifactId("commandapi-bukkit-shade")
                            .version("9.7.1-SNAPSHOT")
                            .relocate("dev{}jorel{}commandapi", "com{}deathmotion{}totemguard{}libs{}commandapi")
                            .resolveTransitiveDependencies(true)
                            .build()
            );
        }
    }
}
