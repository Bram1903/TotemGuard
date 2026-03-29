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

package com.deathmotion.totemguard.common.antivpn;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.api3.config.key.impl.MessagesKeys;
import com.deathmotion.totemguard.api3.event.impl.TGUserVPNDetectionEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.VPNData;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGUserVPNDetectionEventImpl;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

@Getter
public class AntiVPNRepositoryImpl {

    private final ConfigRepositoryImpl configRepository;
    private final CacheRepositoryImpl cacheRepository;
    private final EventRepositoryImpl eventRepository;
    private final AlertRepositoryImpl alertRepository;
    private final MessageService messageService;
    private final Logger logger;

    private boolean enabled;
    private String apiKey = "";
    private @Nullable AntiVPNAdapter antiVPNAdapter;
    private boolean shouldBlock;

    public AntiVPNRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.configRepository = platform.getConfigRepository();
        this.cacheRepository = platform.getCacheRepository();
        this.eventRepository = platform.getEventRepository();
        this.alertRepository = platform.getAlertRepository();
        this.messageService = platform.getMessageService();
        this.logger = platform.getLogger();
        reload();
    }

    public void reload() {
        Config config = configRepository.config(ConfigFile.CONFIG);

        enabled = config.getBoolean(ConfigKeys.VPN_ENABLED);
        apiKey = config.getString(ConfigKeys.VPN_API_KEY);
        shouldBlock = config.getBoolean(ConfigKeys.VPN_BLOCK);

        String providerName = config.getString(ConfigKeys.VPN_PROVIDER);

        antiVPNAdapter = AntiVPNProviders.AntiVPNAdapters.stream()
                .filter(adapter -> adapter.getName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElse(null);

        if (antiVPNAdapter == null) {
            logger.severe("Invalid anti-VPN provider: " + providerName);
            enabled = false;
            return;
        }

        logger.info("Registered " + antiVPNAdapter.getName() + " as anti-VPN provider.");
    }

    public void validateConnection(TGPlayer player) {
        if (!enabled || antiVPNAdapter == null) {
            return;
        }

        String ip = player.getUser().getAddress().getAddress().getHostAddress();

        VPNData cachedData = cacheRepository.getVPNData(ip);
        if (cachedData != null) {
            handleVpnResult(player, ip, cachedData.vpn());
            return;
        }

        boolean vpn = antiVPNAdapter.isVpn(ip);
        cacheRepository.saveVPNData(ip, new VPNData(vpn));
        handleVpnResult(player, ip, vpn);
    }

    private void handleVpnResult(TGPlayer player, String ip, boolean vpn) {
        player.setVpn(vpn);

        if (!vpn) {
            return;
        }

        TGUserVPNDetectionEvent event = eventRepository.post(new TGUserVPNDetectionEventImpl(player, ip));
        if (event.isCancelled()) {
            return;
        }


        alertRepository.broadcast(messageService.getString(MessagesKeys.ANTI_VPN_ALERT));
        if (shouldBlock) {
            // TODO: Disconnect the player
        }
    }
}
