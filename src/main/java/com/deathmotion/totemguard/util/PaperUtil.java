package com.deathmotion.totemguard.util;

public class PaperUtil {
    public static boolean isPaper() {
        return hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");
    }

    public static boolean hasAdventure() {
        return hasClass("io.papermc.paper.adventure.PaperAdventure") || hasClass("net.kyori.adventure.platform.bukkit.BukkitAudience");
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
