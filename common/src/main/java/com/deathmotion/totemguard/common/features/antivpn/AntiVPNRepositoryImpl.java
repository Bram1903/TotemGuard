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

package com.deathmotion.totemguard.common.features.antivpn;

import com.deathmotion.totemguard.api.event.impl.TGUserVPNDetectionEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.config.schema.AntiVpnOptions;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGUserVPNDetectionEventImpl;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class AntiVPNRepositoryImpl {

    private static final Duration MEMORY_CACHE_TTL = Duration.ofMinutes(30);

    private final ConfigRepositoryImpl configRepository;
    private final CacheRepositoryImpl cacheRepository;
    private final DatabaseRepositoryImpl databaseRepository;
    private final EventRepositoryImpl eventRepository;
    private final AlertRepositoryImpl alertRepository;
    private final MessageService messageService;
    private final Logger logger;

    private boolean enabled;
    private boolean shouldBlock;
    private @Nullable AntiVPNAdapter activeAdapter;
    private long persistentFreshnessMillis = TimeUnit.DAYS.toMillis(30);

    public AntiVPNRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.configRepository = platform.getConfigRepository();
        this.cacheRepository = platform.getCacheRepository();
        this.databaseRepository = platform.getDatabaseRepository();
        this.eventRepository = platform.getEventRepository();
        this.alertRepository = platform.getAlertRepository();
        this.messageService = platform.getMessageService();
        this.logger = platform.getLogger();
        reload();
    }

    private static byte @NotNull [] sha256(@NotNull String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String redactIp(String ip) {
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) return ip.substring(0, lastDot) + ".x";
        int lastColon = ip.lastIndexOf(':');
        if (lastColon > 0) return ip.substring(0, lastColon) + ":xxxx";
        return "<redacted>";
    }

    public void reload() {
        AntiVpnOptions opts = configRepository.configView().antiVpn();
        this.shouldBlock = opts.block();

        long retentionDays = configRepository.configView().database().retentionVpnDays();
        if (retentionDays > 0) this.persistentFreshnessMillis = TimeUnit.DAYS.toMillis(retentionDays);

        if (!opts.enabled()) {
            this.enabled = false;
            this.activeAdapter = null;
            return;
        }

        AntiVPNAdapter adapter = AntiVPNProviders.byName(opts.provider());
        if (adapter == null) {
            logger.severe("Anti-VPN provider \"" + opts.provider() + "\" is unknown — disabling. "
                    + "Available: " + AntiVPNProviders.availableNames());
            this.enabled = false;
            this.activeAdapter = null;
            return;
        }

        adapter.configure(opts.apiKey());
        if (adapter.requiresApiKey() && (opts.apiKey() == null || opts.apiKey().isBlank())) {
            logger.severe("Anti-VPN provider \"" + adapter.getName()
                    + "\" requires an API key but anti-vpn.api-key is empty — disabling.");
            this.enabled = false;
            this.activeAdapter = null;
            return;
        }

        this.activeAdapter = adapter;
        this.enabled = true;
        logger.info("Anti-VPN enabled with provider " + adapter.getName());
    }

    public void validateConnection(TGPlayer player) {
        AntiVPNAdapter adapter = this.activeAdapter;
        if (!enabled || adapter == null) return;

        String ip = extractIp(player);
        if (ip == null) return;

        Boolean cached = readCache(ip);
        if (cached != null) {
            handleVpnResult(player, ip, cached);
            return;
        }

        Boolean lookup;
        try {
            lookup = adapter.lookup(ip);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    "Anti-VPN provider " + adapter.getName() + " lookup failed for "
                            + redactIp(ip) + " — defaulting to not-VPN", ex);
            return;
        }
        if (lookup == null) return;

        writeCaches(ip, lookup);
        handleVpnResult(player, ip, lookup);
    }

    private @Nullable String extractIp(TGPlayer player) {
        try {
            InetAddress address = player.getUser().getAddress().getAddress();
            if (address == null) return null;
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()) {
                return null;
            }
            return address.getHostAddress();
        } catch (Exception ex) {
            return null;
        }
    }

    private @Nullable Boolean readCache(String ip) {
        Boolean memory = cacheRepository.getAndRefresh(CacheKeys.vpn(ip), CacheCodecs.BOOLEAN, MEMORY_CACHE_TTL);
        if (memory != null) return memory;

        if (!databaseRepository.isConnected()) return null;
        try {
            Boolean persistent = databaseRepository.findVpnCache(sha256(ip), persistentFreshnessMillis);
            if (persistent != null) {
                cacheRepository.put(CacheKeys.vpn(ip), persistent, CacheCodecs.BOOLEAN, MEMORY_CACHE_TTL);
            }
            return persistent;
        } catch (Exception ex) {
            logger.log(Level.FINE, "Anti-VPN persistent cache lookup failed", ex);
            return null;
        }
    }

    private void writeCaches(String ip, boolean vpn) {
        cacheRepository.put(CacheKeys.vpn(ip), vpn, CacheCodecs.BOOLEAN, MEMORY_CACHE_TTL);

        if (!databaseRepository.isConnected()) return;
        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            try {
                databaseRepository.upsertVpnCache(sha256(ip), vpn);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to persist VPN cache row", ex);
            }
        });
    }

    private void handleVpnResult(TGPlayer player, String ip, boolean vpn) {
        player.setVpn(vpn);
        if (!vpn) return;

        TGUserVPNDetectionEvent event = eventRepository.post(new TGUserVPNDetectionEventImpl(player, ip));
        if (event.isCancelled()) return;

        alertRepository.broadcast(messageService.getString(MessagesKeys.ANTI_VPN_ALERT, player));

        if (!shouldBlock) return;

        PlatformPlayer platformPlayer = player.getPlatformPlayer();
        if (platformPlayer == null) return;
        platformPlayer.kick(messageService.getComponent(MessagesKeys.ANTI_VPN_KICK, player, null, null));
    }
}
