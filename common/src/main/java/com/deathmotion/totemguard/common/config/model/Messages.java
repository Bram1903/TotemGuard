package com.deathmotion.totemguard.common.config.model;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

@Getter
public class Messages {

    private final Component prefix;
    private final Component alertsEnabled;
    private final Component alertsDisabled;
    private final Component alertBrand;

    public Messages(ConfigurationNode config) throws SerializationException {
        this.prefix = config.node("prefix").get(Component.class);
        this.alertsEnabled = config.node("alerts-enabled").get(Component.class);
        this.alertsDisabled = config.node("alerts-disabled").get(Component.class);
        this.alertBrand = config.node("alert-brand").get(Component.class);
    }
}

