/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.common.vpn;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.VPNData;
import com.deathmotion.totemguard.common.player.TGPlayer;

public class VPNRepositoryImpl {

    private final CacheRepositoryImpl cache;

    private boolean enabled;
    private String api_key = "";
    private Adapter adapter;
    private boolean shouldBlock;

    public VPNRepositoryImpl() {
        this.cache = TGPlatform.getInstance().getCacheRepository();
        reload();
    }

    public void reload() {
        TGPlatform platform = TGPlatform.getInstance();
        Config config = platform.getConfigRepository().config(ConfigFile.CONFIG);

        enabled = config.getBoolean(ConfigKeys.VPN_ENABLED);
        api_key = config.getString(ConfigKeys.VPN_API_KEY);
        shouldBlock = config.getBoolean(ConfigKeys.VPN_BLOCK);

        Adapter configAdapter = Providers.adapters.stream()
                .filter(adapter -> adapter.getName().equalsIgnoreCase(config.getString(ConfigKeys.VPN_PROVIDER)))
                .findFirst()
                .orElse(null);

        if (configAdapter != null) {
            adapter = configAdapter;
            platform.getLogger().info("Registered " + adapter.getName() + " as anti-VPN provider.");
            return;
        }

        platform.getLogger().severe("Invalid anti-VP provider: " + config.getString(ConfigKeys.VPN_PROVIDER));
        enabled = false;
        adapter = null;
    }

    public void validateConnection(TGPlayer player) {
        if (!enabled) return;
        if (adapter == null) return;

        String ip = player.getUser().getAddress().getAddress().getHostAddress();

        VPNData vpnData = cache.getVPNData(ip);
        if (vpnData != null) {
            player.setVpn(vpnData.vpn());
            if (!vpnData.vpn()) return;
            handle(player);
            return;
        }

        boolean isVpn = adapter.isVpn(ip);
        player.setVpn(isVpn);
        cache.saveVPNData(ip, new VPNData(isVpn));

        if (isVpn) handle(player);
    }

    public void handle(TGPlayer player) {
        TGPlatform.getInstance().getAlertRepository().broadcast(">" + player.getName() + " is using a VPN!");

        // Call a VPN detection event

        if (!shouldBlock) {
        }
        // TODO: Disconnect the player
    }
}
