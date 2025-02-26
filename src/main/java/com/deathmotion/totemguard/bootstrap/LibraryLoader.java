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
                        .build(),
                Library.builder()
                        .groupId("net{}jodah")
                        .artifactId("expiringmap")
                        .version("0.5.11")
                        .relocate("net{}jodah{}expiringmap", "com{}deathmotion{}totemguard{}libs{}expiringmap")
                        .build(),
                Library.builder()
                        .groupId("com{}j256{}ormlite")
                        .artifactId("ormlite-jdbc")
                        .version("6.1")
                        .relocate("com{}j256{}ormlite", "com{}deathmotion{}totemguard{}libs{}ormlite")
                        .build());

        // Check if running on PaperMC and if the server version is newer than 1.20.4
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_4) && TotemGuard.getInstance().isPaper()) {
            libraryManager.loadLibraries(
                    Library.builder()
                            .groupId("dev{}jorel")
                            .artifactId("commandapi-bukkit-shade-mojang-mapped")
                            .version("9.7.1-SNAPSHOT")
                            .relocate("dev{}jorel{}commandapi", "com{}deathmotion{}totemguard{}libs{}commandapi")
                            .build()
            );
        } else {
            libraryManager.loadLibraries(
                    Library.builder()
                            .groupId("dev{}jorel")
                            .artifactId("commandapi-bukkit-shade")
                            .version("9.7.1-SNAPSHOT")
                            .relocate("dev{}jorel{}commandapi", "com{}deathmotion{}totemguard{}libs{}commandapi")
                            .build()
            );
        }
    }
}
