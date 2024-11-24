package com.deathmotion.totemguard.messaging;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface ProxyAlertMessenger {

    default void start() {

    }

    default void stop() {

    }

    void sendAlert(@NotNull Component alert);

}
