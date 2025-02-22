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

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
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
                    .resolveTransitiveDependencies(true)
                    .build(),
            Library.builder()
                    .groupId("io{}lettuce")
                    .artifactId("lettuce-core")
                    .version("6.5.1.RELEASE")
                    .resolveTransitiveDependencies(false)
                    .build(),
            Library.builder()
                    .groupId("net{}jodah")
                    .artifactId("expiringmap")
                    .version("0.5.11")
                    .resolveTransitiveDependencies(false)
                    .build(),
            Library.builder()
                    .groupId("io{}ebean")
                    .artifactId("ebean-core")
                    .version("15.8.0")
                    .resolveTransitiveDependencies(true)
                    .build(),
            Library.builder()
                    .groupId("io{}ebean")
                    .artifactId("ebean-datasource")
                    .version("9.0")
                    .resolveTransitiveDependencies(true)
                    .build(),
            Library.builder()
                    .groupId("io{}ebean")
                    .artifactId("ebean-migration")
                    .version("14.2.0")
                    .resolveTransitiveDependencies(false)
                    .build(),
            Library.builder()
                    .groupId("io{}ebean")
                    .artifactId("ebean-platform-h2")
                    .version("15.8.0")
                    .resolveTransitiveDependencies(false)
                    .build(),
            Library.builder()
                    .groupId("io{}ebean")
                    .artifactId("ebean-platform-mysql")
                    .version("15.8.0")
                    .resolveTransitiveDependencies(false)
                    .build(),
            Library.builder()
                    .groupId("com{}h2database")
                    .artifactId("h2")
                    .version("2.3.232")
                    .resolveTransitiveDependencies(false)
                    .build(),
            Library.builder()
                    .groupId("mysql")
                    .artifactId("mysql-connector-java")
                    .version("8.0.30")
                    .resolveTransitiveDependencies(false)
                    .build()
    );

    if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_4)) {
      libraryManager.loadLibraries(
              Library.builder()
                      .groupId("dev{}jorel")
                      .artifactId("commandapi-bukkit-shade-mojang-mapped")
                      .version("9.7.1-SNAPSHOT")
                      .resolveTransitiveDependencies(true)
                      .build()
      );
    }
    else {
      libraryManager.loadLibraries(
              Library.builder()
                      .groupId("dev{}jorel")
                      .artifactId("commandapi-bukkit-shade")
                      .version("9.7.1-SNAPSHOT")
                      .resolveTransitiveDependencies(true)
                      .build()
      );
    }
  }
}
