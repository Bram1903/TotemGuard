# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Writing style

Do not use em dashes (—) or semicolons (;) in prose, comments, commit messages, or documentation in this repo. Use
periods, commas, or parentheses instead. (This rule is about written prose. Java/Kotlin statement-terminator
semicolons are obviously fine.)

## Project

TotemGuard is a Minecraft anticheat plugin (v3.0 branch — WIP rewrite). It targets Paper/Folia servers via a Bukkit
plugin, Fabric servers via a Fabric mod, and proxies (Velocity / BungeeCord) via an optional bridge plugin. Packet
interception is done through PacketEvents; commands use the Incendo Cloud framework. The public API artifact is
published to `https://maven.pvphub.me/bram` as `totemguard-api`.

## Build & run

Gradle wrapper, Java 21 toolchain for most modules; the Fabric platform requires JDK 25 (because
`cloud-fabric 2.0.0-beta.16` ships Java 25 bytecode). Use the wrapper (`./gradlew`) for everything.

- Build all shaded jars: `./gradlew build`. Outputs land in the **root** `build/` directory (not per-module
  `build/libs`): `TotemGuard-<Platform>-<version>.jar`, `TotemGuard-Loader-<version>.jar`,
  `TotemGuard-Bridge-<version>.jar`. The plain `jar` task is disabled on shaded modules.
- Build a single artifact: `./gradlew :platforms:bukkit:shadowJar`, `:platforms:fabric:shadowJar`,
  `:loader:plugin:shadowJar`, `:bridge:plugin:shadowJar`.
- Clean: `./gradlew clean` (root `clean` deletes the top-level `build/` where shaded jars live).
- Run a Paper dev server (plain plugin): `./gradlew :platforms:bukkit:runServer` — Paper 1.21.11 with
  PacketEvents/ViaVersion/ViaBackwards/PlaceholderAPI/EssentialsX/LuckPerms into `run/paper/1.21.11/`.
- Run Folia: `./gradlew :platforms:bukkit:runFolia`.
- Run a Paper dev server **through the loader**: `./gradlew :loader:plugin:runServer` — drops into
  `loader/plugin/run/paper/<mc>/` and seeds `plugins/TotemGuard-Loader/local/` with the freshly shaded Bukkit jar so
  `/tgloader load LOCAL` works.
- Run a Velocity dev proxy (bridge plugin): `./gradlew :bridge:plugin:runVelocity`.
- Publish the API: `./gradlew :api:publish` (needs `PVPHUB_MAVEN_USERNAME` / `PVPHUB_MAVEN_SECRET`).
- Local dev services: `docker-compose up` starts MariaDB (3306, db `TotemGuard`, root/`password`) and Redis (6379).
- **Native JNI bridge** (loader): `./gradlew :loader:plugin:compileNative` — uses **`zig cc`** (must be on PATH) to
  cross-compile `src/main/c/native.c` for linux/win32/darwin into `src/main/resources/natives/`. zig is used on every
  host OS so the produced binaries are reproducible regardless of who runs the build.

No unit test suite exists. JUnit tasks are configured with `failOnNoDiscoveredTests = false`. The `tests/` subprojects
(currently `api-bukkit-test-plugin`) are sample plugins demonstrating the public API, not test harnesses.

## Module layout

- `api/` — public API (`com.deathmotion.totemguard.api.*`), published Maven artifact, independently versioned
  (`1.0.0-SNAPSHOT`), compiled to Java 21. Interfaces only.
- `common/` — platform-agnostic implementation. Owns `TGPlatform` (the singleton wiring every repository/service),
  all check logic under `common/check/`, and all features under `common/features/` (alerts, history, mods,
  punishments, monitor, stats, update, follow, teleport, discord, integration). Compiles to Java 17.
- `integrity/` — startup-time jar SHA-256 verifier (`JarIntegrityChecker`). Used by both the plugin and the loader.
  Compiled to Java 17 with `disableAutoTargetJvm()` so it can be loaded from a Java 8 environment if needed.
- `loader/host/` — loader↔plugin contract types (`TGPluginHost`, `TGPluginEntry`, `TGPluginHandle`, `LoaderController`,
  `UpdateTarget`, `Platform`). Class identity is shared across loader and plugin classloaders via `ApiClassInjector`,
  so consumers should **not** import these types as a normal dependency — they must come from the loader's classloader.
- `loader/plugin/` — the fleet-aware update system itself. Downloads/stages/applies TotemGuard plugin jars via sources
  (GitHub, Modrinth, LOCAL), coordinates rollouts across a Redis-backed fleet (`fleet/`, `catalog/`), and class-loads
  the plugin jar through a custom `TGPluginClassLoader` + JNI `NativeClassLoader` (`defineClass` shim, built with
  `zig cc`). Single command surface: `/tgloader <status|peers|versions|search|import|plugin|load|stage|apply|rollout>`.
- `bridge/protocol/` — wire format shared between TotemGuard and the proxy bridge plugin. Java 17, no third-party
  deps. Edit here when adding cross-process packets.
- `bridge/plugin/` — optional proxy bridge plugin (Velocity + BungeeCord in one shaded jar) that improves
  player-presence accuracy and powers same-proxy `/tg teleport`. Lettuce + the bridge protocol + integrity checker;
  Velocity/Bungee are compileOnly.
- `platforms/bukkit/` — Paper/Folia plugin (`TGBukkit extends JavaPlugin`, `TGBukkitPlatform extends TGPlatform`).
  `folia-supported: true`; `load: POSTWORLD`; hard-depends on `packetevents`. The Bukkit shadow jar also embeds bstats
  and cloud-paper.
- `platforms/fabric/` — Fabric mod (`TGFabric`, entrypoint declared in `fabric.mod.json`). Uses fabric-loom, JDK 25
  toolchain, embeds adventure-platform-fabric + cloud-fabric + fabric-permissions-api + mysql-jdbc.
- `tests/api-bukkit-test-plugin/` — sample plugin demonstrating the public API; **not** a test harness.
- `build-logic/` — `includeBuild` with four Gradle convention plugins under `build-logic/src/main/kotlin/` (note: at
  the kotlin root, not inside a `totemguard/` subpackage):
    - `totemguard.java-conventions` — Lombok + JetBrains annotations, JDK 21 toolchain, Java 17 release, and
      `processResources` `expand()` of `${version}` / `${description}` into `plugin.yml`, `fabric.mod.json`,
      `bungee.yml`, `velocity-plugin.json` (with the `+<gitHash>` stripped so manifests stay reproducible).
    - `totemguard.shadow-conventions` — relocations under `com.deathmotion.totemguard.common.libs.*`, jar minimization,
      shadow output goes to root `build/`, and writes the SHA-256 integrity entry after shading.
    - `totemguard.loader-shadow-conventions` — same as above but for the loader jar; relocates
      `com.deathmotion.totemguard.integrity` → `com.deathmotion.totemguard.loader.libs.integrity` so the loader and the
      plugin do not collide on the integrity class.
    - `totemguard.proxy-shadow-conventions` — bridge-jar variant.
    - `tg-version` — task that generates a `TGVersions.java`-style class from the project version into a generated
      source set. Configured per-module via the `tgVersion { packageName; className }` extension (defaults to
      `com.deathmotion.totemguard.common.util.TGVersions`).
- `gradle/libs.versions.toml` — all dependency versions. Edit here, not in individual module `build.gradle.kts`.
- `docs/showcase/` — screenshots/media only.

## Architecture

**Platform abstraction.** `TGPlatform` (in `common/`) is an abstract singleton owning every subsystem (
`ConfigRepository`, `PlayerRepository`, `CheckManager`, `CommandManager`, `RedisRepository`, `GuiManager`,
`EventRepository`, `BridgeManager`, `FleetCacheLifecycle`, …). Concrete platforms (`TGBukkitPlatform`, the Fabric
equivalent) wire platform-specific implementations of `Scheduler`, `PlatformPlayerFactory`, `Sender`, and return the
Incendo Cloud `CommandManager<Sender>`. Bootstrap order: `onLoad` → integrity check → `new TGXxxPlatform(host)` →
`commonOnInitialize()`; `onEnable` → `commonOnEnable()` (instantiates all repositories, registers PacketEvents
listeners, publishes the public `TGPlatformAPI` via `TotemGuard.init(...)`).

**Loader vs standalone.** Every platform plugin can be driven either standalone (registered directly with the server) or
via the TotemGuard Loader. The plugin learns which mode it's in from `TGPluginHost.managedByLoader()`. In loader mode,
the loader-plugin owns the registered `JavaPlugin` (or `ModContainer`) and instantiates `TGPluginEntry` via a custom
`TGPluginClassLoader` + native `defineClass` (so hot-reload works without leaking the previous classloader). API types
are injected into the loader's classloader by `ApiClassInjector` so class identity (e.g. `TGUserFlagEvent.class`) stays
stable across reloads. `loader/host` exists specifically to share those types between the two classloaders — do not
treat it as a normal compile dependency from `common/`.

**Checks.** Detection logic lives in `common/check/impl/`, grouped by category: `autototem/`, `inventory/`, `manual/`,
`mods/`, `protocol/`, `tick/`. Every check extends `CheckImpl` and implements one of `PacketCheck`, `EventCheck`,
`ExtendedCheck`, or `ManualCheck` — which interface determines which dispatch table in `CheckManagerImpl` it lands in.
Metadata comes from `@CheckData(name, description, type, experimental)`; tick-end ordering from `@RequiresTickEnd`.
`CheckManagerImpl` **hard-codes every check in its constructor** (`new AutoTotemA(player)` etc.) — new checks must be
registered there explicitly; there is no auto-discovery. Violations flow through `CheckImpl.fail(...)` →
`TGUserFlagEventImpl` → `AlertRepository` / `PunishmentRepository`. Bypass permission pattern:
`TotemGuard.Bypass.<checkName>`.

**Event system.** Two layers: (1) public API events in `api.event.impl.*` dispatched via `EventRepository`; (2)
internal events under `common/event/` for cross-feature coordination. Packet listeners
(`PacketCheckManagerListener`, join/quit, `GuiPacketListener`) are registered directly with PacketEvents' event manager
from `TGPlatform#commonOnEnable`.

**Config.** YAML files (`config.yml`, `checks.yml`, `messages.yml`, `mods.yml`) bundled in
`common/src/main/resources/`. Typed-key system under `common/config/` (`ConfigKeys`, `MessagesKeys`, …) with
snapshot/view separation (`ConfigService`, `ConfigSnapshot`, `ConfigView`) and migrations under `config/migration/`.
When adding a config value, add the key alongside the YAML — do not just edit the YAML.

**Fleet / cluster mode.** Redis-backed coordination across multiple TotemGuard nodes: `FleetCache` /
`RedisFleetCache` / `FleetCacheLifecycle` in `common/fleet/`, `RedisRepository` for pub/sub, follow state in
`features/follow/FollowStore` (Redis-backed). Lettuce supports cluster topology. The loader has its own fleet layer
(`loader/plugin/fleet/RolloutCoordinator`, `FleetBroker`) used to roll out plugin-jar updates in a staggered way.

**Proxy bridge.** When the bridge plugin is installed on a Velocity/BungeeCord proxy, it advertises proxy topology and
routes RPC requests over Redis (`bridge/plugin/common/redis`, `rpc/`, `state/`). The backend plugins discover the proxy
via `common/network/bridge/BridgeManager`. The wire format is in `bridge/protocol/` — bump it carefully.

**Shading & integrity.** `shadow-conventions` relocates Cloud, bstats, Lettuce, Netty, reactor, redis clients, Hikari,
errorprone/jspecify annotations, geantyref, etc. under `com.deathmotion.totemguard.common.libs.*` (loader uses
`com.deathmotion.totemguard.loader.libs.*`). After shading, every entry except the integrity entry itself is hashed
into `META-INF/totemguard/integrity.sha256`; `JarIntegrityChecker.verifyCurrentJar()` validates this at startup and
refuses to enable the plugin or loader if tampered with. **Don't repack a built jar** — it invalidates the
fingerprint. If you ever need to inspect the integrity logic, see `build-logic/src/main/kotlin/totemguard/build/JarIntegrity.kt`
(write side) and `integrity/src/main/java/.../JarIntegrityChecker.java` (verify side).

**Versioning.** Root version is `3.0.0` + `+<git short hash>` + `-SNAPSHOT` (see root `build.gradle.kts`). The
`tg-version` convention plugin generates `com.deathmotion.totemguard.common.util.TGVersions` (and API-side
`TGAPIVersions`) at build time. The Java convention plugin expands `${version}` into plugin manifests during
`processResources`, with the `+<hash>` stripped so manifests stay stable across rebuilds.
