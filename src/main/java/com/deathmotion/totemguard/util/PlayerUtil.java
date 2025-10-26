package com.deathmotion.totemguard.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlayerUtil {

    /*
        Tries to retrieve an offline player as efficiently as possible.
     */
    public static @NotNull OfflinePlayer getOfflinePlayer(String name) {
        OfflinePlayer p;

        p = Bukkit.getPlayerExact(name);
        if (p != null) return p;

        p = Bukkit.getOfflinePlayerIfCached(name);
        if (p != null) return p;

        return Bukkit.getOfflinePlayer(name);
    }
}
