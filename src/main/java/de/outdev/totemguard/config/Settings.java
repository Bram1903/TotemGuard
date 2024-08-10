package de.outdev.totemguard.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@Configuration
@Getter
public final class Settings {

    @Comment("Prefix: Sets the command prefix for the plugin.")
    private String prefix = "&e&lAUTOTOTEM &8➟ ";

    @Comment("\nCheck Prefix: Sets the prefix for the /check command.")
    private String checkPrefix = "&6&lCHECK &8➟ ";

    @Comment("\nWebhook settings:")
    private Webhook webhook = new Webhook();

    @Comment("\nExtra Flags: Toggles the checks for S: Sneaking, B: Blocking, M: sprinting, swimming, climbing (SBM).")
    private boolean toggleExtraFlags = true;

    @Comment("\nAutomatic Normal Checks: Toggles automatic normal checks.")
    private boolean toggleAutomaticNormalChecks = true;

    @Comment("\nCheck Time: Amount of time the /check command waits for a retotem. (in ticks)")
    private int checkTime = 5;

    @Comment("\nNormal Check Time: Sets the interval (in ms) for normal checks.")
    private int normalCheckTimeMs = 300;

    @Comment("\nClick Time Difference [Experimental]: Measures the amount of time the hacked client takes to move a totem from the inventory slot to the offhand. \nRecommended value: true ")
    private boolean clickTimeDifference = true;

    @Comment("\nAdvanced System Check: Enables an advanced system check that calculates the real totem time making the flag more accurate. \nRecommended value: false")
    private boolean advancedSystemCheck = false;

    @Comment("\nTrigger amount: The flag is only triggered if this value (in ms) is reached. (Advanced System Check)")
    private int triggerAmountMs = 75;

    @Comment("\nDamage on /check: Toggles damage on /check command to ensure a more accurate result.")
    private boolean toggleDamageOnCheck = true;

    @Comment("\nDamage Amount on /check: Amount of damage to inflict on check.")
    private int damageAmountOnCheck = 0;

    @Comment("\nDetermines when the plugin should stop for checking a player.")
    private Determine determine = new Determine();

    @Comment("\nA system that automatically punishes a player after they reach a specific number of AutoTotem flags.")
    private Punish punish = new Punish();

    @Configuration
    @Getter
    public static class Webhook {
        @Comment("Enable and/or disable the webhook implementation.")
        private boolean enabled = false;

        @Comment("\nWebhook URL: The URL of the webhook to send notifications to.")
        private String url = "https://discord.com/api/webhooks/your_webhook_url";

        @Comment("\nWebhook Name: Name of webhook.")
        private String name = "TotemGuard";

        @Comment("\nWebhook Embed color: Color of the webhook embed (in hex).")
        private String color = "#d9b61a";

        @Comment("\nWebhook Title: Brief description about what the webhook is about.")
        private String title = "TotemGuard";

        @Comment("\nEdit the message that contains the information about the report, you can use placeholders such as %player%, %ping%, and more. Supports Markdown.")
        private String[] description = {
                "**Player:** %player%",
                "**Ping:** %ping%",
                "**Retotemed in:** %retotem_time%",
                "**Real Time: %real_retotem_time%",
                "**Extra Flags:** %moving_status%",
                "",
                "**Client Brand:** %brand%",
                "**Flags:** %flag_count%/%punish_after%",
                "**TPS:** %tps%"
        };

        @Comment("\nWebhook Image: Sets the image that is displayed in the embed.")
        private String image = "https://minotar.net/avatar/user/%player%.png";

        @Comment("\nWebhook Profile Image: Sets the image of the embed's profile.")
        private String profileImage = "https://i.imgur.com/hqaGO5H.png";

        @Comment("\nWebhook Timestamp: Displays the time that this embed was sent at.")
        private boolean timestamp = true;
    }

    @Configuration
    @Getter
    public static class Determine {
        @Comment("Minimum TPS.")
        private double minTps = 15.0;

        @Comment("\nMaximum Ping.")
        private int maxPing = 250;
    }

    @Configuration
    @Getter
    public static class Punish {
        @Comment("Enable or disable punishment system.")
        private boolean enabled = false;

        @Comment("\nPunish After: Determines how many flags a player can accumulate before executing the punishment command.")
        private int punishAfter = 10;

        @Comment("\nRemove Flags Min: Interval (in minutes) at which flags are reset globally for all players.")
        private int removeFlagsMin = 30;

        @Comment("\nPunish Command: Command executed when a player reaches the 'punish_after' limit.")
        private String punishCommand = "ban %player% 1d AutoTotem";
    }
}
