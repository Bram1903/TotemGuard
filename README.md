## TotemGuard, AutoTotem AntiCheat

TotemGuard is a plugin specifically designed to detect players using the 'AutoTotem' hack. Developed by Asleepp & OutDev, this plugin ensures fair play on your server by checking for unauthorized totem usage.

### Features

- **Permission-Based Commands**: Ensure only authorized users can execute checks.
- **Configurable Checks**: Customize the duration and conditions for totem checks.
- **Webhook Integration** (not added yet): Send alerts to a Discord webhook with detailed information.
- **Advanced System Check**: Calculate retotem time using the player's ping for more accurate detection.
- **Damage on Check**: Optionally damage players to ensure accurate results.
- **Extra Flags**: Monitor additional player actions like sneaking, blocking, sprinting, swimming, and climbing.
- **Automatic Punish System**: Automatically penalizes players who accumulate too many flags.
  - **`punish`**: Enable or disable the automatic punishment system.
  - **`punish_after`**: Number of flags before applying punishment.
  - **`remove_flags_min`**: Interval (in minutes) for globally resetting all players' flags.
  - **`punish_command:`**: Command executed when the flag limit is reached. `%player%` is replaced with the player’s name.

### Installation

1. Download the TotemGuard plugin.
2. Place the plugin `.jar` file into your server's `plugins` directory.
3. Start or restart your server to load the plugin.
4. Configure the plugin by editing the `config.yml` file located in the TotemGuard folder.
5. Reload the plugin using `/totemguard reload` to apply any changes made to the configuration.

### Configuration

Here’s the default `config.yml` with explanations for each setting:

```yaml
#  ___________     __                   ________                       .___
#  \__    ___/____/  |_  ____   _____  /  _____/ __ _______ _______  __| _/
#    |    | /  _ \   __\/ __ \ /     \/   \  ___|  |  \__  \\_  __ \/ __ |
#    |    |(  <_> )  | \  ___/|  Y Y  \    \_\  \  |  // __ \|  | \/ /_/ |
#    |____| \____/|__|  \___  >__|_|  /\______  /____/(____  /__|  \____ |
#                           \/      \/        \/           \/           \/

# Plugin Configuration File

# General Information:
# This configuration file allows you to customize the settings for the plugin.
# Each setting is explained below.

# Developed by: Asleepp & OutDev

# Prefix: Sets the command prefix for the plugin.
prefix: "&e&lAUTOTOTEM &8➟ "

# Check Prefix: Sets the prefix for the /check command.
check_prefix: "&6&lCHECK &8➟ "

# Command/Alert permissions: Permissions required to use commands and receive alerts.
command_permissions: totemguard.admin
check_permission: totemguard.check
alert_permissions: totemguard.alert

# Webhook settings:
webhook:
  # Enable and/or disable the webhook implementation.
  enabled: false
  # Webhook URL: The URL of the webhook to send notifications to.
  url: "https://discord.com/api/webhooks/your_webhook_url"
  # Webhook Name: Name of webhook
  name: "TotemGuard"
  # Webhook Embed color: Color of the webhook embed (in hex).
  color: "#d9b61a"
  # Webhook Title: Brief description about what the webhook is about.
  title: "TotemGuard AutoTotem Check"
  # Edit the message that contains the information about the report, you can use placeholders such as %player%, %ping%, and more. Supports Markdown.
  # Using more than one string will add a new line.
  description:
    - "**Player:** %player%"
    - "**Ping:** %ping%"
    - "**Retotemed in** %retotem_time%"
    - "**Is Moving:** %moving_status%"
    - "**Confidence:** %confidence%"
    - "**TPS:** %tps%"
    - ""
    - "**Total Flags:** %total_flags%"
  # Webhook Image: Sets the image that is displayed in the embed.
  # This example displays the player's head using the Minotar website.
  image: "https://minotar.net/avatar/user/%player%.png"
  # Webhook Profile Image: Sets the image of the embed's profile
  profile_image: "https://example.com/profile_image.png" # TODO ADD IMAGE HERE
  # Webhook Timestamp: Displays the time that this embed was sent at.
  timestamp: true

# Extra Flags: Toggles the checks for S: Sneaking, B: Blocking, M: sprinting, swimming, climbing (SBM).
toggle_extra_flags: true

# Automatic Normal Checks: Toggles automatic normal checks.
toggle_automatic_normal_checks: true

# Check Time: Amount of time the /check command waits for a retotem. (in ticks)
# Changing this is not recommended as it can make checking inaccurate. (Recommended: 5)
# It does help with determining the real time it took to retotem. (if set higher)
check_time: 5

# Normal Check Time: Sets the interval (in ms) for normal checks.
# Valid values are between 50 and 250 ms.
# This value is recommended to be set higher than 'trigger_amount_ms' if
# 'advanced_system_check' is set to true (recommended: 300+)
normal_check_time_ms: 175

# Advanced System Check: Enables an advanced system check that calculates the real totem time making the flag more accurate.
# This check uses the player's ping to determine the actual totem usage time. Generally, this flag provides
# more accurate results, especially if a delay is used in the AutoTotem mod. Note that some flags might not
# be recognized due to ping spikes or inaccurate ping readings from the server.

# Trigger amount: The flag is only triggered if this value (in ms) is reached.
# Make sure to adjust your 'normal_check_time_ms' and 'trigger_amount_ms' in the configuration file accordingly. (300+)
advanced_system_check: false
trigger_amount_ms: 75

# Damage on /check: Toggles damage on /check command to ensure a more accurate result.
# If set to true and damage_amount_on_check: 0, this check will damage the player by 80% their hearts (recommended)
toggle_damage_on_check: true
damage_amount_on_check: 0

# Determines when the plugin should stop for checking a player.
# This is to ensure no false flags being created by low tps or ping.
min_tps: 15.0
max_ping: 250

# A system that automatically punishes a player after they reach a specific number of AutoTotem flags.
# Punish After: Determines how many flags a player can accumulate before executing the punishment command.
# Remove Flags Min: Interval (in minutes) at which flags are reset globally for all players.
# Punish Command: Command executed when a player reaches the 'punish_after' limit.
# The %player% variable will be replaced with the player's name.
punish: false
punish_after: 10
remove_flags_min: 30
punish_command: "ban %player% 1d AutoTotem"

# Save your changes and reload the plugin to apply the new settings. (/totemguard reload)

# Discord: @asleepp, @outdev
# GitHub: @outdev0, @asleeepp
```

### Commands

- **`/check <player>`**: Manually check a player for unauthorized totem usage.  
  **Permission**: `totemguard.check`

- **`/totemguard reload`**: Reload the plugin configuration.  
  **Permission**: `totemguard.admin`

### Permissions

- **`totemguard.admin`**: Allows access to admin commands.
- **`totemguard.check`**: Allows checking players for unauthorized totem usage.
- **`totemguard.alert`**: Receives alerts from the plugin.

### Webhook Integration 
(not added yet)

TotemGuard can send notifications to a Discord webhook. Configure the webhook settings in the `config.yml`:

- **`enabled`**: Toggle webhook notifications on or off.
- **`url`**: The URL of your Discord webhook.
- **`name`**: The name of the webhook.
- **`color`**: Embed color in hex format.
- **`title`**: The title of the webhook message.
- **`description`**: The content of the webhook message. Supports placeholders and Markdown.
- **`image`**: URL of the image displayed in the embed.
- **`profile_image`**: URL of the webhook's profile image.
- **`timestamp`**: Whether to include a timestamp in the webhook message.

### Advanced Configuration

- **`toggle_extra_flags`**: Enable or disable checks for additional actions like sneaking, blocking, sprinting, swimming, and climbing.
- **`toggle_automatic_normal_checks`**: Enable or disable automatic normal checks.
- **`check_time`**: The duration in ticks for the /check command.
- **`normal_check_time_ms`**: Interval in milliseconds for normal checks (50 to 250 ms). 
