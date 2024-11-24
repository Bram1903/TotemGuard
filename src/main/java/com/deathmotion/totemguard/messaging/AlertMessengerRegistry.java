package com.deathmotion.totemguard.messaging;

import com.deathmotion.totemguard.TotemGuard;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Map.entry;

public class AlertMessengerRegistry {
    private static final Map<String, Function<TotemGuard, ProxyAlertMessenger>> types = Map.ofEntries(
        entry("redis", RedisProxyMessenger::new),
        entry("plugin-messaging", PluginMessageProxyMessenger::new)
    );

    private AlertMessengerRegistry() {
    }

    public void registerMessenger(@NotNull String identifier, @NotNull Function<TotemGuard, ProxyAlertMessenger> supplier) {
        types.put(identifier, supplier);
    }

    public static Optional<Function<TotemGuard, ProxyAlertMessenger>> getMessengerSupplier(@NotNull String identifier) {
        return Optional.of(types.get(identifier));
    }

    public static Optional<ProxyAlertMessenger> getMessenger(@NotNull String identifier, @NotNull TotemGuard instance) {
        return getMessengerSupplier(identifier).map((func) -> func.apply(instance));
    }

    public static boolean contains(@NotNull String identifier) {
        return types.containsKey(identifier);
    }

}
