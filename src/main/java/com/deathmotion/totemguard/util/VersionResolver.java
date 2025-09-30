package com.deathmotion.totemguard.util;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import org.bukkit.Bukkit;

public class VersionResolver {

    @Getter
    private static final ServerVersion minimumSupportedVersion = ServerVersion.V_1_16_5;

    public boolean isSupportedVersion() {
        ServerVersion v = resolveVersionNoCache();
        return v != ServerVersion.ERROR && !v.isOlderThan(minimumSupportedVersion);
    }

    private ServerVersion resolveVersionNoCache() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        for (final ServerVersion val : ServerVersion.reversedValues()) {
            if (bukkitVersion.contains(val.getReleaseName())) {
                return val;
            }
        }

        return ServerVersion.ERROR;
    }
}
