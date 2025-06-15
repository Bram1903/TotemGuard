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

package com.deathmotion.totemguard.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Webhooks {
    @Comment("Webhook Alert Settings")
    private AlertSettings alert = new AlertSettings();

    @Comment("\nWebhook Punishment Settings")
    private PunishmentSettings punishment = new PunishmentSettings();

    @Configuration
    @Getter
    public abstract static class WebhookSettings {
        @Comment("Enable and/or disable the webhook implementation.")
        private boolean enabled = false;

        @Comment("\nWebhook URL: The URL of the webhook to send notifications to.")
        private String url = "https://discord.com/api/webhooks/your_webhook_url";

        @Comment("\nClient Name: Name of the client.")
        private String name = "TotemGuard";

        @Comment("\nWebhook Embed color: Color of the webhook embed (in hex).")
        private String color;

        @Comment("\nWebhook Title: Brief description about what the webhook is about. (Like Alert, Punishment, etc.)")
        private String title;

        @Comment("\nWebhook Profile Image: Sets the image of the embed's profile.")
        private String profileImage = "https://imgur.com/a/9IUVips";

        @Comment("\nWebhook Timestamp: Displays the time that this embed was sent at.")
        private boolean timestamp = true;

        @Comment("\nWebhook Footer: Sets the server name as the footer.")
        private boolean footer = true;

        public WebhookSettings(String title, String color) {
            this.title = title;
            this.color = color;
        }
    }

    @Configuration
    @Getter
    public static class AlertSettings extends WebhookSettings {
        public AlertSettings() {
            super("TotemGuard Alert", "#d9b61a");
        }
    }

    @Configuration
    @Getter
    public static class PunishmentSettings extends WebhookSettings {
        public PunishmentSettings() {
            super("TotemGuard Punishment", "#d60010");
        }
    }
}
