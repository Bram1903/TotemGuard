// File: com/deathmotion/totemguard/common/alert/AlertBuilder.java
package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.api.config.key.impl.MessagesKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class AlertBuilder {

    private AlertBuilder() {
    }

    public static String build(CheckImpl check, int violations, @Nullable String debugInfo) {
        Config messages = TGPlatform.getInstance()
                .getConfigRepository()
                .config(ConfigFile.MESSAGES);

        String message = messages.getString(MessagesKeys.ALERTS_MESSAGE);

        if (debugInfo != null) {
            message += messages.getString(MessagesKeys.ALERTS_DEBUG);
        }

        Map<String, Object> extras = Map.of(
                "tg_check_violations", violations,
                "tg_check_debug", debugInfo == null ? "" : debugInfo
        );

        return TGPlatform.getInstance()
                .getPlaceholderRepository()
                .replace(message, check.player, check, extras);
    }
}
