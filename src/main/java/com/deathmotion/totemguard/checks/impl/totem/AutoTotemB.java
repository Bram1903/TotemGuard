package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.ICheck;
import com.deathmotion.totemguard.config.Settings;
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

public class AutoTotemB extends Check implements ICheck, PacketListener, Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemUseTimes;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemReEquipTimes;
    private final ConcurrentHashMap<UUID, Boolean> expectingReEquip;
    private final ConcurrentHashMap<UUID, Double> smoothedConfidence;
    private final ConcurrentHashMap<UUID, Integer> consistencyViolations;

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Re-toteming too consistently", true);

        this.plugin = plugin;
        this.totemUseTimes = new ConcurrentHashMap<>();
        this.totemReEquipTimes = new ConcurrentHashMap<>();
        this.expectingReEquip = new ConcurrentHashMap<>();
        this.smoothedConfidence = new ConcurrentHashMap<>();
        this.consistencyViolations = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Event handler for when a totem is used
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Check if the player used a totem and start tracking re-equip
        if (player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING) {
            recordTotemEvent(totemUseTimes, player.getUniqueId());
            expectingReEquip.put(player.getUniqueId(), true);
        }
    }

    // Event handler for when a player interacts with their inventory
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        boolean isExpectingReEquip = expectingReEquip.getOrDefault(playerId, false);

        // Track re-equip if a totem is moved into the player's inventory
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING && isExpectingReEquip) {
            recordTotemEvent(totemReEquipTimes, playerId);
            expectingReEquip.put(playerId, false);
            checkPlayerConsistency(player);
        }
    }

    // Packet listener for offhand item swap
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        if (!(packet.getAction().equals(DiggingAction.SWAP_ITEM_WITH_OFFHAND))) return;

        Player player = (Player) event.getPlayer();

        // Delayed check to see if the player re-equipped the totem in the offhand
        // We need to do this because Bukkit only handles packets a tick later
        FoliaScheduler.getEntityScheduler().runDelayed(player, plugin, (o) -> {
            if (player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) return;
            if (!expectingReEquip.getOrDefault(player.getUniqueId(), false)) return;

            recordTotemEvent(totemReEquipTimes, player.getUniqueId());
            expectingReEquip.put(player.getUniqueId(), false);
            checkPlayerConsistency(player);
        }, null, 1);
    }

    public void resetData() {
        totemUseTimes.clear();
        totemReEquipTimes.clear();
        expectingReEquip.clear();
        smoothedConfidence.clear();
        consistencyViolations.clear();
    }

    public void resetData(UUID uuid) {
        totemUseTimes.remove(uuid);
        totemReEquipTimes.remove(uuid);
        expectingReEquip.remove(uuid);
        smoothedConfidence.remove(uuid);
        consistencyViolations.remove(uuid);
    }

    // Record a totem event (use or re-equip) and maintain a maximum of 10 events
    private void recordTotemEvent(ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> map, UUID playerId) {
        ConcurrentLinkedDeque<Long> deque = map.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(System.nanoTime());

        if (deque.size() > 10) {
            deque.pollFirst();  // Keep only the last 10 events
        }
    }

    // Check the player's consistency in re-equipping totems
    private void checkPlayerConsistency(Player player) {
        UUID playerId = player.getUniqueId();

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            ConcurrentLinkedDeque<Long> useTimes = totemUseTimes.get(playerId);
            ConcurrentLinkedDeque<Long> reEquipTimes = totemReEquipTimes.get(playerId);

            // Require at least 3 use and re-equip events to perform the check
            if (useTimes == null || reEquipTimes == null || useTimes.size() < 3 || reEquipTimes.size() < 3) {
                return;
            }

            long[] intervals = new long[useTimes.size()];
            int i = 0;

            // Calculate intervals between totem use and re-equip events
            for (Long useTime : useTimes) {
                Long reEquipTime = reEquipTimes.toArray(new Long[0])[i];

                if (useTime == null || reEquipTime == null) {
                    return;
                }

                intervals[i++] = reEquipTime - useTime;
            }

            double mean = calculateMean(intervals);
            double standardDeviation = calculateStandardDeviation(intervals, mean);

            final Settings.Checks.AutoTotemB settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemB();

            // Calculate an adaptive threshold for standard deviation
            double adaptiveThreshold = settings.getStandardDeviationThreshold() * 1_000_000L * (1 + (10 - useTimes.size()) * 0.1);

            if (standardDeviation < adaptiveThreshold) {
                // Increment violations based on consistency
                double violationWeight = standardDeviation < settings.getStandardDeviationThreshold() * 500_000L ? 2.0 : 1.0;
                int violations = consistencyViolations.getOrDefault(playerId, 0) + (int) violationWeight;
                consistencyViolations.put(playerId, violations);

                // Exponentially weighted smoothing of confidence
                double previousConfidence = smoothedConfidence.getOrDefault(playerId, 0.0);
                double currentConfidence = (double) violations / useTimes.size();
                double alpha = 0.7;  // Smoothing factor
                double smoothedValue = alpha * currentConfidence + (1 - alpha) * previousConfidence;

                smoothedConfidence.put(playerId, smoothedValue);

                double meanInMs = mean / 1_000_000.0;
                double standardDeviationInMs = standardDeviation / 1_000_000.0;

                plugin.debug(player.getName() + " - Standard Deviation: " + standardDeviationInMs + " ms - Confidence: " + smoothedValue + " - Mean: " + meanInMs + " ms");

                // Flag the player if confidence exceeds the threshold
                if (smoothedValue > settings.getConfidenceThreshold()) {
                    Component details = Component.text()
                            .append(Component.text("Standard deviation: ", NamedTextColor.GOLD))
                            .append(Component.text(String.format("%.2fms", standardDeviationInMs), NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("Confidence: ", NamedTextColor.GOLD))
                            .append(Component.text(String.format("%.2f", smoothedValue), NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("Mean: ", NamedTextColor.GOLD))
                            .append(Component.text(String.format("%.2fms", meanInMs), NamedTextColor.GRAY))
                            .build();

                    consistencyViolations.put(playerId, 0); // Reset violations
                    flag(player, details, plugin.getConfigManager().getSettings().getChecks().getAutoTotemB());
                }
            } else {
                // Decrease violation count if behavior becomes inconsistent
                int currentViolations = consistencyViolations.getOrDefault(playerId, 0);
                consistencyViolations.put(playerId, Math.max(0, currentViolations - 1));
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