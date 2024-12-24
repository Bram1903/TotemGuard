/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Settings {
    @Comment("API: Weather or not the API is enabled.")
    private boolean API = true;

    @Comment("\nServer Name: The name of the server. (Used for alerts, webhooks, API, etc.)")
    private String Server = "Default";

    @Comment("\nBypass: Weather or not players with the permission 'totemguard.bypass' can bypass checks.")
    private boolean Bypass = false;

    @Comment("Announce client brand: Weather or not the client brand should be announced upon a player joining.")
    private boolean AnnounceClientBrand = false;

    @Comment("\nProxy Alert Settings:")
    private ProxyAlerts Proxy = new ProxyAlerts();

    @Comment("\nUpdate Checker Settings:")
    private UpdateChecker UpdateChecker = new UpdateChecker();

    @Configuration
    @Getter
    public static class ProxyAlerts {
        @Comment({
                "Proxy messaging method",
                "How should be send and receive messages from sibling servers?",
                "Options:",
                " - plugin-messaging (Will use plugin messaging through player connections.)",
                " - redis (Requires further configuration in the 'redis' section below.)"
        })
        private String Method = "plugin-messaging";

        @Comment("\nChannel: The channel to send and receive alerts.")
        private String Channel = "totemguard";

        @Comment("\nWhen enabled, the plugin will send alerts to other servers connected to the proxy.")
        private boolean Send = true;

        @Comment("\nWhen enabled, the plugin will receive alerts from other servers connected to the proxy.")
        private boolean Receive = true;

        @Comment("\nRedis Configuration")
        private RedisConfiguration Redis = new RedisConfiguration();

        @Configuration
        @Getter
        public static class RedisConfiguration {
            private String Host = "localhost";
            private int Port = 6379;
            private String Username = "default";
            private String Password = "yourPassword";
        }
    }

    @Configuration
    @Getter
    public static class UpdateChecker {
        @Comment("Enable and/or disable the update checker.")
        private boolean Enabled = true;

        @Comment("\nPrint to Console: Prints the update message to the console.")
        private boolean PrintToConsole = true;

        @Comment("\nNotify In-Game: Notifies players with the permission in-game.")
        private boolean NotifyInGame = true;
    }

    @Comment("\nDebug: Enables debug mode (Advanced Users Only).")
    private boolean Debug = false;
}
