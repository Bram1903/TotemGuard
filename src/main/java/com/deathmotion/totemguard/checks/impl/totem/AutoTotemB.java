package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AutoTotemB extends Check implements PacketListener, Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemUseTimes;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemReEquipTimes;
    private final ConcurrentHashMap<UUID, Boolean> expectingReEquip;
    private final ConcurrentHashMap<UUID, Integer> consistencyViolations;

    private static final int VIOLATION_THRESHOLD = 3;
    private static final double STANDARD_DEVIATION_THRESHOLD = 10.0;

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Re-toteming too consistently", true);

        this.plugin = plugin;
        this.totemUseTimes = new ConcurrentHashMap<>();
        this.totemReEquipTimes = new ConcurrentHashMap<>();
        this.expectingReEquip = new ConcurrentHashMap<>();
        this.consistencyViolations = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING) {
            recordTotemEvent(totemUseTimes, player.getUniqueId());
            expectingReEquip.put(player.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        boolean isExpectingReEquip = expectingReEquip.getOrDefault(playerId, false);

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING && isExpectingReEquip) {
            recordTotemEvent(totemReEquipTimes, playerId);
            expectingReEquip.put(playerId, false);
            checkPlayerConsistency(player);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            Player player = (Player) event.getPlayer();

            if (packet.getAction().equals(DiggingAction.SWAP_ITEM_WITH_OFFHAND) && player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
                recordTotemEvent(totemReEquipTimes, player.getUniqueId());
                checkPlayerConsistency(player);
            }
        }
    }

    @Override
    public void resetData() {
        totemUseTimes.clear();
        totemReEquipTimes.clear();
        expectingReEquip.clear();
        consistencyViolations.clear();
    }

    private void recordTotemEvent(ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> map, UUID playerId) {
        ConcurrentLinkedDeque<Long> deque = map.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());

        deque.addLast(System.nanoTime());

        // Ensure the deque size remains at 10, maintaining rolling data
        if (deque.size() > 10) {
            deque.pollFirst();
        }
    }

    private void checkPlayerConsistency(Player player) {
        UUID playerId = player.getUniqueId();

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            ConcurrentLinkedDeque<Long> useTimes = totemUseTimes.get(playerId);
            ConcurrentLinkedDeque<Long> reEquipTimes = totemReEquipTimes.get(playerId);

            plugin.getLogger().info("Checking player " + player.getName() + ": useTimes size = " +
                    (useTimes != null ? useTimes.size() : "null") +
                    ", reEquipTimes size = " +
                    (reEquipTimes != null ? reEquipTimes.size() : "null"));

            if (useTimes == null || reEquipTimes == null || useTimes.size() < 10 || reEquipTimes.size() < 10) {
                plugin.getLogger().info("Early exit: Not enough data for player " + player.getName());
                return;
            }

            long[] intervals = new long[10];
            int i = 0;

            for (Long useTime : useTimes) {
                Long reEquipTime = reEquipTimes.toArray(new Long[0])[i];

                if (useTime == null || reEquipTime == null) {
                    plugin.getLogger().info("Early exit: Null value found in pairs for player " + player.getName());
                    return;
                }

                intervals[i++] = reEquipTime - useTime;
            }

            double mean = calculateMean(intervals);
            double standardDeviation = calculateStandardDeviation(intervals, mean);

            double meanInMs = mean / 1_000_000.0;
            double standardDeviationInMs = standardDeviation / 1_000_000.0;

            plugin.getLogger().info("Player " + player.getName() + " - Mean: " + meanInMs + " ms");
            plugin.getLogger().info("Player " + player.getName() + " - Standard deviation: " + standardDeviationInMs + " ms");

            // Check if the player's standard deviation is below the threshold for consistency
            if (standardDeviationInMs < STANDARD_DEVIATION_THRESHOLD) {
                int violations = consistencyViolations.getOrDefault(playerId, 0) + 1;
                consistencyViolations.put(playerId, violations);

                if (violations >= VIOLATION_THRESHOLD) {
                    Component details = Component.text()
                            .append(Component.text("Standard deviation: ", NamedTextColor.GOLD))
                            .append(Component.text(String.format("%.2f ms", standardDeviationInMs), NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("Mean: ", NamedTextColor.GOLD))
                            .append(Component.text(String.format("%.2f ms", meanInMs), NamedTextColor.GRAY))
                            .build();

                    flag(player, details, plugin.getConfigManager().getSettings().getChecks().getAutoTotemB());
                    consistencyViolations.put(playerId, 0);
                }
            } else {
                // Reset violation count if the player exhibits human-like behavior
                consistencyViolations.put(playerId, 0);
            }
        });
    }

    private double calculateMean(long[] intervals) {
        long sum = 0;
        for (long interval : intervals) {
            sum += interval;
        }
        return (double) sum / intervals.length;
    }

    private double calculateStandardDeviation(long[] intervals, double mean) {
        double varianceSum = 0;
        for (long interval : intervals) {
            varianceSum += Math.pow(interval - mean, 2);
        }
        double variance = varianceSum / intervals.length;
        return Math.sqrt(variance);
    }
}