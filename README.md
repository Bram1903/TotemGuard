<div align="center">
  <h1>TotemGuard</h1>
  <img alt="Build" src="https://github.com/Bram1903/TotemGuard/actions/workflows/gradle.yml/badge.svg">
  <img alt="CodeQL" src="https://github.com/Bram1903/TotemGuard/actions/workflows/codeql.yml/badge.svg">
  <img alt="GitHub Release" src="https://img.shields.io/github/release/Bram1903/TotemGuard.svg">
  <br>
  <a href="https://www.spigotmc.org/resources/totemguard.114851/"><img alt="SpigotMC" src="https://img.shields.io/badge/-SpigotMC-blue?style=for-the-badge&logo=SpigotMC"></a>
  <a href="https://modrinth.com/plugin/totemguard"><img alt="TotemGuard" src="https://img.shields.io/badge/-Modrinth-green?style=for-the-badge&logo=Modrinth"></a>
  <br>
  <a href="https://discord.deathmotion.com"><img alt="Discord" src="https://img.shields.io/badge/-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white"></a>
</div>

## Overview

TotemGuard is a rather simple anti-cheat plugin that tries to detect players who are using the so-called 'AutoTotem' cheat.
This plugin is designed to be as lightweight as possible and for the most parts operates asynchronously. 
It is also designed to be as configurable as possible, allowing server owners to adjust the plugin to their needs.

### Requires PacketEvents

Ensure the [PacketEvents](https://modrinth.com/plugin/packetevents) library is installed on your server.

## Table of Contents

- [Overview](#overview)
    - [Requires PacketEvents](#requires-packetevents)
- [Supported Platforms & Versions](#supported-platforms--versions)
- [Features](#features)
- [Commands](#commands)
- [Permission Nodes](#permission-nodes)
- [Installation](#installation)
- [Compiling From Source](#compiling-from-source)
    - [Prerequisites](#prerequisites)
    - [Steps](#steps)
- [Credits](#credits)
- [License](#license)

## Supported Platforms & Versions

| Platform                   | Supported Versions |
|----------------------------|--------------------|
| Bukkit (Paper, Folia etc.) | 1.18 - 1.21.1      |

## Features

- **Asynchronous** - The plugin is designed to be as lightweight as possible.
  Almost all operations are done
  asynchronously.
  This ensures that the server performance is not affected.
- **Folia Support** - The plugin integrates with [Folia](https://papermc.io/software/folia), which is a Paper fork that
  adds regionised multithreading to the server.
- **Configurable** - The plugin is highly configurable, allowing you to adjust the settings to your liking.
- **Update Checker** - The plugin automatically checks for updates on startup.
  If a new version is available, a message will be sent to the console.

## Commands

- `/totemguard` or `/tg` - Base Command
- `/totemguard alerts` - Toggles alerts on/off.
- `/totemguard reload` - Reloads the configuration file (every single setting supports reloading on runtime).
- `/totemguard check` or `/totemcheck`, `/checktotem` - Removes a totem from a player's inventory and checks if they are being quickly replaced.

## Permission Nodes

Operators (OPs) have these permissions by default:

- `TotemGuard.*` - Grants access to all TotemGuard permissions.
- `TotemGuard.Alerts` - Allows the player to toggle alerts.
- `TotemGuard.Alerts.Others` - Allows the player to toggle alerts for other players.
- `TotemGuard.Check` - Allows the player to check if another player is using AutoTotem.
- `TotemGuard.Reload` - Allows the player to reload the configuration file.
- `TotemGuard.Bypass` - Allows the player to bypass the plugin's checks (if enabled in the configuration).
- `TotemGuard.Update` - Allows the player to get notified about updates.

## Installation

1. **Prerequisites**: Install [PacketEvents](https://modrinth.com/plugin/packetevents).
2. **Download**: Get the latest release from
   the [GitHub release page](https://github.com/Bram1903/TotemGuard/releases/latest).
3. **Installation**: Move the downloaded plugin to your server's plugins directory.
4. **Configuration**: Customize settings in `config.yml`.
5. **Reload**: Run `/totemguard reload` to apply the changes.

## Compiling From Source

### Prerequisites

- Java Development Kit (JDK) version 21 or higher
- [Git](https://git-scm.com/downloads)

### Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Bram1903/TotemGuard.git
   ```

2. **Navigate to Project Directory**:
   ```bash
   cd TotemGuard
   ```

3. **Compile the Source Code**:
   Use the Gradle wrapper to compile and generate the plugin JAR file:

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
