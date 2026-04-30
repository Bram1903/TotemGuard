# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TotemGuard is a Minecraft anticheat plugin (currently on the v3.0 branch, a WIP rewrite). It targets Paper/Folia servers via a Bukkit plugin and Velocity proxies. Packet interception is done through PacketEvents; commands use the Incendo Cloud framework. Public API artifacts are published to `https://maven.pvphub.me/bram` as `totemguard-api`.

## Build & run

Gradle wrapper, Java 21 toolchain, compile target Java 17 (API module compiles to 21). Use the wrapper (`./gradlew`) for everything.

- Build all shaded plugin jars: `./gradlew build` — output lands in `build/TotemGuard-<Platform>-<version>.jar` (not in each module's `build/libs`; see `shadow-conventions`). The plain `jar` task is disabled on shaded modules.
- Build a specific platform: `./gradlew :platforms:bukkit:shadowJar` or `:platforms:velocity:shadowJar`.
- Clean: `./gradlew clean` (the root `clean` deletes the top-level `build/` directory where shaded jars are written).
- Run a Paper dev server: `./gradlew :platforms:bukkit:runServer` — downloads Paper 1.21.11 plus PacketEvents/ViaVersion/ViaBackwards/PlaceholderAPI/EssentialsX/LuckPerms into `run/paper/1.21.11/`.
- Run Folia: `./gradlew :platforms:bukkit:runFolia`.
- Run a Velocity dev proxy: `./gradlew :platforms:velocity:runVelocity`.
- Publish the API module: `./gradlew :api:publish` (requires `PVPHUB_MAVEN_USERNAME` / `PVPHUB_MAVEN_SECRET` env vars).
- Ancillary services for local dev: `docker-compose up` starts MariaDB (3306, db `TotemGuard`, root/`password`) and Redis (6379).

No unit test suite exists yet — JUnit tasks are configured with `failOnNoDiscoveredTests = false`. The `tests/` subprojects (`api-bukkit-test-plugin`, `api-velocity-test-plugin`) are sample plugins demonstrating the public API, not test harnesses.

## Module layout

- `api/` — public API (`com.deathmotion.totemguard.api.*`), published Maven artifact, compiled to Java 21, versioned independently (`1.0.0-SNAPSHOT`). Defines interfaces only: `TotemGuardAPI`, `EventRepository`, `ConfigRepository`, `UserRepository`, etc.
- `common/` — platform-agnostic implementation. Contains `TGPlatform` (abstract singleton that wires every repository/service) and all check logic under `common/check/`.
- `platforms/bukkit/` — Paper/Folia plugin (`TGBukkit` extends `JavaPlugin`, subclasses `TGBukkitPlatform extends TGPlatform`). Plugin name is `TotemGuard`; `folia-supported: true`; `load: POSTWORLD`; hard-depends on `packetevents`.
- `platforms/velocity/` — Velocity plugin (`TGVelocity` / `TGVelocityPlatform`).
- `platforms/bungeecord/`, `platforms/sponge/` — stubs, currently commented out of `settings.gradle.kts`.
- `build-logic/` — `includeBuild` with three Gradle convention plugins: `totemguard.java-conventions` (Lombok + JetBrains annotations, JDK 21 toolchain, Java 17 release, resource `expand()` of `version` into `plugin.yml`/`velocity-plugin.json`), `totemguard.shadow-conventions` (relocations + shaded-jar SHA-256 integrity entry), `tg-version` (generates `TGVersions.java` from the project version).
- `gradle/libs.versions.toml` — all dependency versions. Edit here, not in individual module `build.gradle.kts`.

## Architecture

**Platform abstraction.** `TGPlatform` (in `common/`) is an abstract singleton owning every subsystem (`ConfigRepository`, `PlayerRepository`, `CheckManager`, `CommandManager`, `RedisRepository`, `GuiManager`, `EventRepository`, etc.). Each concrete platform (`TGBukkitPlatform`, `TGVelocityPlatform`) wires platform-specific implementations of `Scheduler`, `PlatformUserFactory`, `Sender`, and returns an Incendo Cloud `CommandManager<Sender>`. Plugin bootstrap: `onLoad` → `new TGXxxPlatform(...)` → `commonOnInitialize()` (jar integrity check); `onEnable` → `commonOnEnable()` (instantiates all repositories, registers PacketEvents listeners, publishes the public `TGPlatformAPI` via `TotemGuard.init(...)`).

**Checks.** All detection logic lives in `common/check/impl/`, grouped by category (`autototem/`, `inventory/`, `mods/`, `protocol/`, `tick/`). Every check extends `CheckImpl` and implements one of `PacketCheck`, `EventCheck`, or `ExtendedCheck` — these three interfaces determine which of the three `ClassToInstanceMap`s in `CheckManagerImpl` it belongs to. Metadata comes from the `@CheckData(name, description, type, experimental)` annotation; tick-end ordering from `@RequiresTickEnd`. `CheckManagerImpl` hard-codes every check in its constructor — **new checks must be registered there** (not auto-discovered). Violations flow through `CheckImpl.fail(...)` → `TGUserFlagEventImpl` → `AlertRepository` / `PunishmentRepository`. Bypass permission pattern: `TotemGuard.Bypass.<checkName>`.

**Event system.** Two layers: (1) public API events in `api.event.impl.*` (`TGUserFlagEvent`, `TGUserJoinEvent`, etc.) dispatched via `EventRepository`; (2) internal events under `common/event/internal/` for cross-check coordination. Packet listeners (`PacketCheckManagerListener`, `PacketPlayerJoinQuit`, `GuiPacketListener`) are registered directly with PacketEvents' event manager in `TGPlatform#commonOnEnable`.

**Config.** YAML files (`config.yml`, `checks.yml`, `messages.yml`, `mods.yml`) are bundled in `common/src/main/resources/`. `ConfigRepositoryImpl` uses a typed-key system (`ConfigValueKey`, `ConfigKeys`, `MessagesKeys` in `api.config.key`) with snapshot/view separation (`ConfigService`, `ConfigSnapshot`, `ConfigView`) and migration support (`ConfigMigration`, `MigrationRegistry`). When adding a config value, add the key to the relevant `*Keys` class in the API module, not just the YAML.

**Shading & integrity.** `totemguard.shadow-conventions` relocates Cloud, bstats, Lettuce, Netty, reactor, etc. under `com.deathmotion.totemguard.common.libs.*`. After shading, a SHA-256 of every entry (except the integrity entry itself) is written to `META-INF/totemguard/integrity.sha256`; `JarIntegrityChecker.verifyCurrentJar()` validates this at startup and refuses to enable the plugin if tampered. Don't repack the jar after build — it invalidates the fingerprint.

**Versioning.** Root version is `3.0.0` + git short hash + `-SNAPSHOT` (see `build.gradle.kts`). The `tg-version` convention plugin generates `com.deathmotion.totemguard.common.util.TGVersions` at build time from this, and the Java convention plugin expands `${version}` into `plugin.yml` / `velocity-plugin.json` during `processResources` (with the git hash stripped so plugin.yml stays stable).
