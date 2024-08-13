package com.strealex.totemguard.config;

import com.strealex.totemguard.checks.ICheckSettings;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@Configuration
@Getter
public final class Settings {

    @Comment("Prefix: Sets the command prefix for the plugin.")
    private String prefix = "&e&lAUTOTOTEM &8➟ ";

    @Comment("\nThe time in minutes at which the plugin should reset the violations.")
    private int resetViolationsInterval = 30;

    @Comment("\nWebhook settings:")
    private Webhook webhook = new Webhook();

    @Comment("\nDetermines when the plugin should stop for checking a player.")
    private Determine determine = new Determine();

    @Comment("\nChecks")
    private Checks checks = new Checks();

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
        private int maxPing = 500;
    }

    @Configuration
    @Getter
    public static class Checks {
        @Comment("AutoTotemA Settings")
        private AutoTotemA autoTotemA = new AutoTotemA();

        @Comment("AutoTotemB Settings")
        private AutoTotemB autoTotemB = new AutoTotemB();

        @Comment("ManualTotemA Settings")
        private ManualTotemA manualTotemA = new ManualTotemA();

        @Configuration
        @Getter
        public static class AutoTotemA implements ICheckSettings {
            private boolean enabled = true;
            private boolean punishable = false;
            private int maxViolations = 5;
            private String[] punishmentCommands = {
                "ban %player% 1d [TotemGuard] Unfair Advantage"
            };

            @Comment("\nNormal Check Time: Sets the interval (in ms) for normal checks.")
            private int normalCheckTimeMs = 1000;

            @Comment("\nClick Time Difference [Experimental]: Measures the amount of time the hacked client takes to move a totem from the inventory slot to the offhand. \nRecommended value: true ")
            private boolean clickTimeDifference = true;

            @Comment("\nAdvanced System Check: Enables an advanced system check that calculates the real totem time making the flag more accurate. \nRecommended value: false")
            private boolean advancedSystemCheck = false;

            @Comment("\nTrigger amount: The flag is only triggered if this value (in ms) is reached. (Advanced System Check)")
            private int triggerAmountMs = 75;

            @Comment("\nClick Time Difference Value: The value (in ms) which anything below will trigger the flag. (Click Time Difference)")
            private int clickTimeDifferenceValue = 75;
        }

        @Configuration
        @Getter
        public static class AutoTotemB implements ICheckSettings {
            private boolean enabled = true;
            private boolean punishable = false;
            private int maxViolations = 5;
            private String[] punishmentCommands = {
                "ban %player% 1d [TotemGuard] Unfair Advantage"
            };
        }

        @Configuration
        @Getter
        public static class ManualTotemA implements ICheckSettings {
            private boolean enabled = true;
            private boolean punishable = false;
            private int maxViolations = 5;
            private String[] punishmentCommands = {
                "ban %player% 1d [TotemGuard] Unfair Advantage"
            };

            @Comment("\nCheck Time: Amount of time the /check command waits for a retotem. (in ticks)")
            private int checkTime = 1000;

            @Comment("\nDamage on /check: Toggles damage on /check command to ensure a more accurate result.")
            private boolean toggleDamageOnCheck = true;

            @Comment("\nDamage Amount on /check: Amount of damage to inflict on check.")
            private int damageAmountOnCheck = 0;
        }
    }
}
