package com.deathmotion.totemguard.common.config.codec.impl;

import com.deathmotion.totemguard.common.config.codec.ComponentCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class NativeCodec implements ComponentCodec {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexCharacter('#')
                    .extractUrls()
                    .build();

    @Override
    public Component deserialize(final String input) {
        return LEGACY.deserialize(input);
    }

    @Override
    public String serialize(final Component component) {
        return LEGACY.serialize(component);
    }
}
