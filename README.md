## TotemGuard

TotemGuard is a plugin specifically designed to detect players using the 'AutoTotem' hack. Developed by Asleepp & OutDev, this plugin ensures fair play on your server by checking for unauthorized totem usage.

### Features

- **Permission-Based Commands**: Ensure only authorized users can execute checks.
- **Configurable Checks**: Customize the duration and conditions for totem checks.
- **Webhook Integration** (not added yet): Send alerts to a Discord webhook with detailed information.
- **Advanced System Check**: Calculate retotem time using the player's ping.
- **Damage on Check**: Optionally damage players to ensure accurate results.
- **Extra Flags**: Monitor additional player actions like sneaking, blocking, and sprinting.
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
normal_check_time_ms: 175

# Advanced System Check: Enables an advanced system check that calculates the retotem time.
# This check uses the player's ping to determine the real retotem time.
# By default, this is true.
advanced_system_check: false

# Damage on /check: Toggles damage on /check command to ensure a more accurate result.
# If set to true and damage_amount_on_check: 0, this check will damage the player by 80% their hearts (recommended)
toggle_damage_on_check: true
damage_amount_on_check: 0

# Determines when the plugin should stop for checking a player.
# This is to ensure no false flags being created by low tps or ping.
min_tps: 15.0
max_ping: 250

# Automatic Punish System: Automatically applies penalties to players who accumulate too many flags.
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

- **`toggle_extra_flags`**: Enable or disable checks for additional actions like sneaking, blocking, and sprinting.
- **`toggle_automatic_normal_checks`**: Enable or disable automatic normal checks.
- **`check_time`**: The duration in ticks for the /check command.
- **`normal_check_time_ms`**: Interval in milliseconds for normal checks (50 to 250 ms).
- **`advanced_system_check`**: Use the player's ping to determine the real retotem time.
- **`toggle_damage_on_check`**: Damage the player during the /check command to ensure accuracy.
- **`damage_amount_on_check`**: The amount of damage to apply during the check. Set to 0 to damage by half the player's health.
- **`min_tps`**: Minimum TPS required to perform checks.
- **`max_ping`**: Maximum ping allowed for a player to be checked.

### Support

For support, you can reach out to the developers on Discord:

- **@asleepp**
- **@outdev**

GitHub:

- **[OutDev](https://github.com/outdev0)**
- **[Asleepp](https://github.com/asleeepp)**
