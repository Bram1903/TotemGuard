# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Writing style

Do not use em dashes or semicolons in prose, comments, commit messages, or documentation in this repo. Use
periods, commas, or parentheses instead. (This rule is about written prose. Java and Kotlin statement-terminator
semicolons are obviously fine.)

## Code documentation

Default to no comments or javadoc on internal code. Well-named identifiers and types already say what the
code does. Add a comment or javadoc only when the WHY is non-obvious, a hidden constraint, a subtle invariant,
a workaround for a specific bug, an architectural decision a reader cannot infer from the code itself. Do
not write javadoc that just restates the method name or parameter list.

The `api/` module is the exception. It is published to Maven as `totemguard-api` and consumed by third-party
plugins, so every public type, method, and parameter gets a full javadoc with nullability annotations
(`@NotNull` / `@Nullable` from JetBrains), `@param` / `@return` tags where they add information, and notes on
thread-safety and side effects where relevant. Treat `api/` javadocs as part of the API surface itself, since
changing them is changing the contract.

## Project

TotemGuard is a Minecraft anticheat plugin (v3.0 branch, a WIP rewrite). It targets Paper or Folia servers
(and Paper-based forks) via a JavaPlugin, Fabric servers via a Fabric mod, and proxies (Velocity, BungeeCord)
via an optional bridge plugin. Spigot and CraftBukkit are not supported (the plugin refuses to enable on
non-Paper). Packet interception is done through PacketEvents. Commands use the Incendo Cloud framework. The
public API artifact is published to `https://maven.pvphub.me/bram` as `totemguard-api`.

## Build & run

Gradle wrapper, Java 21 toolchain for most modules. The Fabric platform requires JDK 25 because
`cloud-fabric 2.0.0-beta.16` ships Java 25 bytecode. Use the wrapper (`./gradlew`) for everything.

- Build all shaded jars: `./gradlew build`. Outputs land in the root `build/` directory (not per-module
  `build/libs`) as `TotemGuard-Paper-<version>.jar`, `TotemGuard-Fabric-<version>.jar`,
  `TotemGuard-Loader-<version>.jar`, and `TotemGuard-Bridge-<version>.jar`. The plain `jar` task is disabled
  on shaded modules.
- Build a single artifact: `./gradlew :platforms:paper:shadowJar`, `:platforms:fabric:shadowJar`,
  `:loader:plugin:shadowJar`, or `:bridge:plugin:shadowJar`.
- Clean: `./gradlew clean`. The root `clean` deletes the top-level `build/` where shaded jars live.
- Run a Paper dev server (plain plugin): `./gradlew :platforms:paper:runServer` (default 1.21.11) or pick a
  version with `:platforms:paper:runServer_<mc>`. Matrix is 1.19.4, 1.20.4, 1.21.1, 1.21.2, 1.21.4, 1.21.11,
  26.1.2. Downloads Paper plus PacketEvents, ViaVersion, ViaBackwards, PlaceholderAPI, EssentialsX, and
  LuckPerms into `run/paper/<mc>/`.
- Run Folia: `./gradlew :platforms:paper:runFolia` or `:platforms:paper:runFolia_<mc>` for the same matrix.
- Run a Fabric dev server: `./gradlew :platforms:fabric:runServer`. Modrinth-resolved
  packetevents-fabric and LuckPerms-Fabric are cached in `build/fabric-mod-cache/` and copied into
  `run/mods/` if missing. `eula.txt` and `server.properties` are seeded automatically.
- Run a Paper dev server through the loader: `./gradlew :loader:plugin:runServer` (same versioned variants).
  Stages the freshly shaded `platforms/paper` jar into `plugins/TotemGuard-Loader/local/` so
  `/tgloader load LOCAL` works.
- Run a Fabric dev server through the loader: `./gradlew :loader:fabric-glue:runServer`. Stages the
  `platforms/fabric` shadow jar into `run/config/totemguard-loader/local/`.
- Run a Velocity dev proxy (bridge plugin): `./gradlew :bridge:plugin:runVelocity`.
- Publish the API: `./gradlew :api:publish`. Needs `PVPHUB_MAVEN_USERNAME` and `PVPHUB_MAVEN_SECRET`.
- Local dev services: `docker-compose up` starts MariaDB (3306, db `TotemGuard`, root and `password`) and
  Redis (6379).
- Native JNI bridge (loader): `./gradlew :loader:plugin:compileNative`. Uses `zig cc` (must be on PATH) to
  cross-compile `src/main/c/native.c` for linux, win32, and darwin into `src/main/resources/natives/`. zig
  runs on every host OS so the produced binaries are reproducible regardless of who runs the build.

No unit test suite exists. JUnit tasks are configured with `failOnNoDiscoveredTests = false`. The `tests/`
subprojects (currently `api-paper-test-plugin`) are sample plugins demonstrating the public API, not test
harnesses.

## Module layout

- `api/`. Public API (`com.deathmotion.totemguard.api.*`), published Maven artifact, independently versioned
  (`1.0.0-SNAPSHOT`), compiled to Java 21. Interfaces only.
- `common/`. Platform-agnostic implementation. Owns `TGPlatform` (the singleton wiring every repository and
  service), all check logic under `common/check/`, and all features under `common/features/` (alerts, history,
  mods, punishments, monitor, stats, update, follow, teleport, discord, integration). Compiles to Java 17.
- `integrity/`. Startup-time jar SHA-256 verifier (`JarIntegrityChecker`). Used by both the plugin and the
  loader. Compiled to Java 17 with `disableAutoTargetJvm()` so it can be loaded from a Java 8 environment if
  needed.
- `loader/host/`. Loader to plugin contract types (`TGPluginHost`, `TGPluginEntry`, `TGPluginHandle`,
  `LoaderController`, `UpdateTarget`, `Platform`). Class identity is shared across loader and plugin
  classloaders via `ApiClassInjector`, so consumers should not import these types as a normal dependency.
  They must come from the loader's classloader.
- `loader/plugin/`. The fleet-aware update system itself. Downloads, stages, and applies TotemGuard plugin
  jars via sources (GitHub, Modrinth, LOCAL), coordinates rollouts across a Redis-backed fleet (`fleet/`,
  `catalog/`), and class-loads the plugin jar through a custom `TGPluginClassLoader` plus a JNI
  `NativeClassLoader` (a `defineClass` shim built with `zig cc`). Single command surface,
  `/tgloader <status|peers|versions|search|import|plugin|load|stage|apply|rollout>`.
- `bridge/protocol/`. Wire format shared between TotemGuard and the proxy bridge plugin. Java 17, no
  third-party deps. Edit here when adding cross-process packets.
- `bridge/plugin/`. Optional proxy bridge plugin (Velocity and BungeeCord in one shaded jar) that improves
  player-presence accuracy and powers same-proxy `/tg teleport`. Lettuce plus the bridge protocol plus the
  integrity checker. Velocity and Bungee are compileOnly.
- `platforms/paper/`. Paper or Folia plugin (`TGPaper extends JavaPlugin`,
  `TGPaperPlatform extends TGPlatform`). `folia-supported: true`, `load: POSTWORLD`, hard-depends on
  `packetevents`. The Paper shadow jar also embeds bstats and cloud-paper. `PaperCompatibility` refuses to
  enable on non-Paper servers (checks for `com.destroystokyo.paper.PaperConfig` or
  `io.papermc.paper.configuration.Configuration`).
- `platforms/fabric/`. Fabric mod (`TGFabric`, entrypoint declared in `fabric.mod.json`). Uses fabric-loom,
  JDK 25 toolchain, embeds adventure-platform-fabric, cloud-fabric, fabric-permissions-api, and mysql-jdbc.
- `tests/api-paper-test-plugin/`. Sample plugin demonstrating the public API. Not a test harness.
- `build-logic/`. `includeBuild` with precompiled convention plugins under
  `build-logic/src/main/kotlin/totemguard/`, grouped into `java/` (toolchain and Lombok variants), `shadow/`
  (relocation + integrity variants for plugin, loader, proxy), and `runs/` (Paper/Folia matrix, Fabric loom
  runs with Modrinth mod staging, Velocity). Root-level scripts cover `root` (version assembly via
  `GitHashValueSource`), `manifest-expand`, `maven-publish`, `loader-stage-platform`, and `tg-version`
  (generates `TGVersions.java` from the project version). Helper Kotlin lives flat under `totemguard/build/`.
- `gradle/libs.versions.toml`. All dependency versions. Edit here, not in individual module
  `build.gradle.kts`.
- `docs/showcase/`. Screenshots and media only.

## Architecture

**Platform abstraction.** `TGPlatform` (in `common/`) is an abstract singleton owning every subsystem
(`ConfigRepository`, `PlayerRepository`, `CheckManager`, `CommandManager`, `RedisRepository`, `GuiManager`,
`EventRepository`, `BridgeManager`, `FleetCacheLifecycle`, and more). Concrete platforms (`TGPaperPlatform`,
the Fabric equivalent) wire platform-specific implementations of `Scheduler`, `PlatformPlayerFactory`, and
`Sender`, and return the Incendo Cloud `CommandManager<Sender>`. Bootstrap order: `onLoad` runs the integrity
check then constructs `new TGXxxPlatform(host)` then `commonOnInitialize()`. `onEnable` runs
`commonOnEnable()`, which instantiates all repositories, registers PacketEvents listeners, and publishes the
public `TGPlatformAPI` via `TotemGuard.init(...)`.

**Loader vs standalone.** Every platform plugin can be driven either standalone (registered directly with the
server) or via the TotemGuard Loader. The plugin learns which mode it is in from
`TGPluginHost.managedByLoader()`. In loader mode, the loader-plugin owns the registered `JavaPlugin` (or
`ModContainer`) and instantiates `TGPluginEntry` via a custom `TGPluginClassLoader` plus native
`defineClass`, so hot-reload works without leaking the previous classloader. API types are injected into the
loader's classloader by `ApiClassInjector` so class identity (for example `TGUserFlagEvent.class`) stays
stable across reloads. `loader/host` exists specifically to share those types between the two classloaders.
Do not treat it as a normal compile dependency from `common/`.

**Checks.** Detection logic lives in `common/check/impl/`, grouped by category, `autototem/`, `inventory/`,
`manual/`, `mods/`, `protocol/`, `tick/`. Every check extends `CheckImpl` and implements one of `PacketCheck`,
`EventCheck`, `ExtendedCheck`, or `ManualCheck`, and the interface determines which dispatch table in
`CheckManagerImpl` it lands in. Metadata comes from `@CheckData(name, description, type, experimental)`.
Tick-end ordering comes from `@RequiresTickEnd`. `CheckManagerImpl` hard-codes every check in its constructor
(`new AutoTotemA(player)` etc.), so new checks must be registered there explicitly. There is no
auto-discovery. Violations flow through `CheckImpl.fail(...)` into `TGUserFlagEventImpl` into
`AlertRepository` or `PunishmentRepository`. Bypass permission pattern, `TotemGuard.Bypass.<checkName>`.

**Event system.** Two layers. Public API events in `api.event.impl.*` dispatched via `EventRepository`, and
internal events under `common/event/` for cross-feature coordination. Packet listeners
(`PacketCheckManagerListener`, join and quit, `GuiPacketListener`) are registered directly with PacketEvents'
event manager from `TGPlatform#commonOnEnable`.

**Config.** YAML files (`config.yml`, `checks.yml`, `messages.yml`, `mods.yml`) bundled in
`common/src/main/resources/`. Typed-key system under `common/config/` (`ConfigKeys`, `MessagesKeys`, and so
on) with snapshot and view separation (`ConfigService`, `ConfigSnapshot`, `ConfigView`) and migrations under
`config/migration/`. When adding a config value, add the key alongside the YAML. Do not just edit the YAML.

**Fleet and cluster mode.** Redis-backed coordination across multiple TotemGuard nodes, `FleetCache`,
`RedisFleetCache`, and `FleetCacheLifecycle` in `common/fleet/`, plus `RedisRepository` for pub-sub, plus
follow state in `features/follow/FollowStore` (Redis-backed). Lettuce supports cluster topology. The loader
has its own fleet layer (`loader/plugin/fleet/RolloutCoordinator`, `FleetBroker`) used to roll out
plugin-jar updates in a staggered way.

**Proxy bridge.** When the bridge plugin is installed on a Velocity or BungeeCord proxy, it advertises proxy
topology and routes RPC requests over Redis (`bridge/plugin/common/redis`, `rpc/`, `state/`). The backend
plugins discover the proxy via `common/network/bridge/BridgeManager`. The wire format is in
`bridge/protocol/`. Bump it carefully.

**Shading and integrity.** `totemguard.shadow.plugin` relocates Cloud, bstats, Lettuce, Netty, reactor, redis
clients, Hikari, errorprone and jspecify annotations, geantyref, and so on under
`com.deathmotion.totemguard.common.libs.*`. `totemguard.shadow.loader` uses
`com.deathmotion.totemguard.loader.libs.*`. `totemguard.shadow.proxy` uses
`com.deathmotion.totemguard.proxybridge.libs.*`. After shading, every entry except the integrity entry itself
is hashed into `META-INF/totemguard/integrity.sha256`. `JarIntegrityChecker.verifyCurrentJar()` validates this
at startup and refuses to enable the plugin or loader if tampered with. Do not repack a built jar, because it
invalidates the fingerprint. If you ever need to inspect the integrity logic, see
`build-logic/src/main/kotlin/totemguard/build/JarIntegrity.kt` (write side) and
`integrity/src/main/java/.../JarIntegrityChecker.java` (verify side).

**Versioning.** Root version is `3.0.0` plus `+<git short hash>` plus `-SNAPSHOT`, assembled by
`totemguard.root` using a `GitHashValueSource` so the value is configuration-cache clean. The `tg-version`
convention plugin generates `com.deathmotion.totemguard.common.util.TGVersions` and the API-side
`TGAPIVersions` at build time. `totemguard.manifest-expand` expands `${version}` into plugin manifests during
`processResources`, with the `+<hash>` stripped so manifests stay stable across rebuilds. Configuration cache
is enabled (`problems=warn`) and type-safe project accessors are on, so prefer `projects.api` over
`project(":api")` in new build scripts.
