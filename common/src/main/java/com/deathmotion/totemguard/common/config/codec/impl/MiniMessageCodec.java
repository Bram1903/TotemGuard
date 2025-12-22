package com.deathmotion.totemguard.common.config.codec.impl;

import com.deathmotion.totemguard.common.config.codec.ComponentCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MiniMessageCodec implements ComponentCodec {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public Component deserialize(final String input) {
        return miniMessage.deserialize(input);
    }

    @Override
    public String serialize(final Component component) {
        return miniMessage.serialize(component);
    }
}
