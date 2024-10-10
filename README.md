<div align="center">
  <h1>TotemGuard</h1>
  <img alt="Build" src="https://github.com/Bram1903/TotemGuard/actions/workflows/gradle.yml/badge.svg">
  <img alt="CodeQL" src="https://github.com/Bram1903/TotemGuard/actions/workflows/codeql.yml/badge.svg">
  <img alt="GitHub Release" src="https://img.shields.io/github/release/Bram1903/TotemGuard.svg">
  <br>
  <a href="https://www.spigotmc.org/resources/totemguard.119385/"><img alt="SpigotMC" src="https://img.shields.io/badge/-SpigotMC-blue?style=for-the-badge&logo=SpigotMC"></a>
  <a href="https://modrinth.com/plugin/totemguard"><img alt="TotemGuard" src="https://img.shields.io/badge/-Modrinth-green?style=for-the-badge&logo=Modrinth"></a>
  <br>
  <a href="https://discord.deathmotion.com"><img alt="Discord" src="https://img.shields.io/badge/-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white"></a>
</div>

## Overview

TotemGuard is a lightweight anti-cheat plugin designed to detect players using AutoTotem. It operates asynchronously to minimize server impact and offers extensive configurability, enabling server owners to tailor the plugin to their specific needs.

### Prerequisites

TotemGuard requires the [PacketEvents](https://modrinth.com/plugin/packetevents) library to function. Ensure it is installed on your server.

## Table of Contents

- [Overview](#overview)
    - [Prerequisites](#prerequisites)
- [Showcase](#showcase)
- [Supported Platforms & Versions](#supported-platforms--versions)
- [Checks](#checks)
- [Features](#features)
- [Commands](#commands)
- [Permission Nodes](#permission-nodes)
- [Installation](#installation)
- [Compiling From Source](#compiling-from-source)
    - [Prerequisites](#prerequisites)
    - [Steps](#steps)
- [Credits](#credits)
- [License](#license)

## Showcase

![Demo](docs/showcase/showcase.png)

## Supported Platforms & Versions

| Platform                        | Supported Versions |
|---------------------------------|--------------------|
| Paper, Folia, and related forks | 1.18 - 1.21.1      |

## Checks

### AutoTotem

- **AutoTotemA** - Click time difference
- **AutoTotemB** - Impossible standard deviation
- **AutoTotemC** - Impossible consistency difference
- **AutoTotemD** - Suspicious re-totem packet sequence
- **AutoTotemE** - Impossible low outliers
- **AutoTotemF** - Invalid interactions during inventory close

### BadPackets

- **BadPacketsA** - Opt-out message in a mod configuration channel
- **BadPacketsB** - Banned client brand

### ManualTotem

- **ManualTotemA** - Time difference between replacement after totem removal

## Features

- **Performance** - Asynchronous operations ensure minimal impact on server performance.
- **Database Support** - Compatible with both MySQL and SQLite.
- **Folia Integration** - Supports [Folia](https://papermc.io/software/folia) for regionized multithreading.
- **Webhooks** - Send alerts and punishments to a Discord webhook.
- **Highly Configurable** - Adjust nearly every setting during runtime to fit your server's needs.
- **Update Checker** - Automatically checks for updates on startup.
- **Bypass Permission** - Allows players with `TotemGuard.Bypass` to bypass checks.
- **Bedrock Exception** - Automatically ignores Bedrock Edition players to prevent false positives.
- **BetterReload Support** - Integrates with [BetterReload](https://modrinth.com/plugin/betterreload) for seamless configuration reloading.

## Commands

- `/totemguard` or `/tg` - Main command for TotemGuard.
- `/totemguard reload` - Reload the plugin configuration.
- `/totemguard alerts` - Toggle alerts for the player.
- `/totemguard alerts <player>` - Toggle alerts for another player.
- `/totemguard profile` - Display the player's profile.
- `/totemguard stats` - Show plugin statistics.
- `/totemguard clearlogs` - Clear the logs.
- `/totemguard track <player>` - Tracks the player.
- `/totemguard database trim` - Trim the database.
- `/totemguard database clear` - Clear the database.
- `/totemcheck <player>` - Check the player for AutoTotem.

## Permission Nodes

Operators (OPs) have these permissions by default, except `TotemGuard.Debug`:

- `TotemGuard.*` - Access to all TotemGuard permissions.
- `TotemGuard.Staff` - Access to `TotemGuard.Check`, `TotemGuard.Alerts`, and `TotemGuard.Profile`.
- `TotemGuard.Databases.*` - Access to all database-related commands.
- `TotemGuard.Reload` - Access to the `/totemguard reload` command.
- `TotemGuard.Check` - Access to the `/totemcheck` command.
- `TotemGuard.Alerts` - Access to the `/totemguard alerts` command.
- `TotemGuard.Alerts.Others` - Toggle alerts for other players.
- `TotemGuard.Profile` - Access to the `/totemguard profile` command.
- `TotemGuard.Stats` - Access to the `/totemguard stats` command.
- `TotemGuard.ClearLogs` - Access to the `/totemguard clearlogs` command.
- `TotemGuard.Track` - Access to the `/totemguard track` command.
- `TotemGuard.Bypass` - Bypass the plugin's checks.
- `TotemGuard.Update` - Receive update notifications.
- `TotemGuard.Database.Trim` - Access to the `/totemguard database trim` command.
- `TotemGuard.Database.Clear` - Access to the `/totemguard database clear` command.
- `TotemGuard.Debug` - View debug messages.

## Installation

1. **Prerequisites**: Ensure [PacketEvents](https://modrinth.com/plugin/packetevents) is installed.
2. **Download**: Get the latest release from the [GitHub release page](https://github.com/Bram1903/TotemGuard/releases/latest).
3. **Install**: Place the plugin JAR file in your server's `plugins` directory.
4. **Configure**: Customize the `config.yml` file as needed.
5. **Reload**: Apply the changes using `/totemguard reload`.

## Compiling From Source

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- [Git](https://git-scm.com/downloads)

### Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Bram1903/TotemGuard.git
   ```
2. **Navigate to the Project Directory**:
   ```bash
   cd TotemGuard
   ```
3. **Compile the Source Code**:
   Use the Gradle wrapper to build the plugin:

   <details>
   <summary><strong>Linux / macOS</strong></summary>

   ```bash
   ./gradlew build
   ```
   </details>
   <details>
   <summary><strong>Windows</strong></summary>

   ```cmd
   .\gradlew build
   ```
   </details>

## Credits

Special thanks to:

- **[@Retrooper](https://github.com/retrooper)**: Author of [PacketEvents](https://github.com/retrooper/packetevents).

## License

This project is licensed under the [GPL3 License](LICENSE).