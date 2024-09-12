package com.deathmotion.totemguard.checks.impl.crystalaura;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.manager.EntityCacheManager;
import com.deathmotion.totemguard.util.MathUtil;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class CrystalAuraA extends Check implements PacketListener, Listener {

    private final TotemGuard plugin;
    private final EntityCacheManager entityCacheManager;

    // Map to store the last attack time and a list of times since the last attack for each player.
    private final ConcurrentHashMap<UUID, Long> lastAttack = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> attackTimesList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> standardDeviations = new ConcurrentHashMap<>();

    public CrystalAuraA(TotemGuard plugin) {
        super(plugin, "CrystalAuraA", "Impossible crystal attack consistency", true);

        this.plugin = plugin;
        this.entityCacheManager = plugin.getEntityCacheManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            int entityId = packet.getEntityId();
            User user = event.getUser();
            entityCacheManager.getCachedEntity(user.getUUID(), entityId).ifPresent(cachedEntity -> {
                long currentTime = System.currentTimeMillis();

                Long lastAttackTime = lastAttack.getOrDefault(user.getUUID(), null);
                if (lastAttackTime != null) {
                    long timeSinceLastAttack = currentTime - lastAttackTime;

                    if (timeSinceLastAttack > 2000) {
                        lastAttack.put(user.getUUID(), currentTime);
                        return;
                    }

                    // Add the timeSinceLastAttack to the list for the player.
                    ConcurrentLinkedDeque<Long> attackTimes = addAttackTime(user.getUUID(), timeSinceLastAttack);
                    checkSuspiciousActivity((Player) event.getPlayer(), attackTimes);
                }

                lastAttack.put(user.getUUID(), currentTime);
            });
        }
    }

    private void checkSuspiciousActivity(Player player, ConcurrentLinkedDeque<Long> attackTimes) {
        if (attackTimes.size() < 5) return;

        double stdev = MathUtil.getStandardDeviation(attackTimes);

        plugin.debug("Player: " + player.getName());
        plugin.debug("Standard Deviation: " + stdev);

        var settings = plugin.getConfigManager().getSettings().getChecks().getCrystalAuraA();

        // Add the standard deviation to the deque for this player
        ConcurrentLinkedDeque<Double> stdDevs = standardDeviations.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentLinkedDeque<>());
        stdDevs.addLast(stdev);

        // Limit the deque size to store only the last 30 standard deviation values
        if (stdDevs.size() > 10) {
            stdDevs.pollFirst(); // Remove the oldest value to keep only the last 30
        }

        if (stdDevs.size() < 9) return;

        double mean = MathUtil.getMean(stdDevs);
        plugin.debug("Mean: " + mean);

        if (mean <= 1) {
            flag(player, createComponent(mean), settings);
        }
    }

    private ConcurrentLinkedDeque<Long> addAttackTime(UUID uuid, long time) {
        ConcurrentLinkedDeque<Long> attackTimes = attackTimesList.computeIfAbsent(uuid, k -> new ConcurrentLinkedDeque<>());

        attackTimes.addLast(time);
        if (attackTimes.size() > 5) {
            attackTimes.pollFirst();
        }

        return attackTimes;
    }

    private Component createComponent(double mean) {
        return Component.text()
                .append(Component.text("Recent Stdev Mean: ", NamedTextColor.GRAY))
                .append(Component.text(mean + "ms", NamedTextColor.GOLD))
                .build();
    }

    @Override
    public void resetData() {
        lastAttack.clear();
        attackTimesList.clear();
        standardDeviations.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        lastAttack.remove(uuid);
        attackTimesList.remove(uuid);
        standardDeviations.remove(uuid);
    }
}
