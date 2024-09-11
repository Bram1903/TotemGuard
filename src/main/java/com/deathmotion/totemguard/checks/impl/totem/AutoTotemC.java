package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.TotemEventListener;
import com.deathmotion.totemguard.checks.impl.totem.processor.TotemProcessor;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AutoTotemC extends Check implements TotemEventListener {
    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> sdHistoryMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> consistentSDCountMap = new ConcurrentHashMap<>();

    public AutoTotemC(TotemGuard plugin) {
        super(plugin, "AutoTotemC", "Impossible re-totem consistency", true);
        this.plugin = plugin;
        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        // Get the player's SD history or create a new one
        ConcurrentLinkedDeque<Double> sdHistory = sdHistoryMap.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentLinkedDeque<>());

        List<Long> recentIntervals = totemPlayer.getLatestIntervals(4);
        double standardDeviation = MathUtil.trim(2, MathUtil.getStandardDeviation(recentIntervals));

        // Add the current SD to the history
        sdHistory.addLast(standardDeviation);
        if (sdHistory.size() > 4) {
            sdHistory.pollFirst();
        }

        // Only proceed if we have at least three SDs to compare
        if (sdHistory.size() >= 2) {
            List<Double> sdList = new ArrayList<>(sdHistory);
            List<Double> differences = new ArrayList<>();

            // Calculate differences between consecutive SD values
            for (int i = 1; i < sdList.size(); i++) {
                differences.add(Math.abs(sdList.get(i) - sdList.get(i - 1)));
            }

            double averageSDDifference = MathUtil.trim(2, MathUtil.getMean(differences));

            //plugin.debug(player.getName() + " - Average SD Difference: " + averageSDDifference + "ms");
            Settings.Checks.AutoTotemC settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemC();

            // Check if the average SD difference is below the threshold
            if (averageSDDifference < settings.getConsistentSDRange()) {
                int consecutiveConsistentSDCount = consistentSDCountMap.getOrDefault(player.getUniqueId(), 0) + 1;
                consistentSDCountMap.put(player.getUniqueId(), consecutiveConsistentSDCount);

                if (consecutiveConsistentSDCount >= 1) {
                    consistentSDCountMap.remove(player.getUniqueId());
                    sdHistoryMap.remove(player.getUniqueId());  // Reset history after flagging
                    flag(player, createComponent(averageSDDifference), settings);
                }
            } else {
                // Reset the count if the average SD difference is above the range
                consistentSDCountMap.put(player.getUniqueId(), 0);
            }
        }
    }

    @Override
    public void resetData() {
        consistentSDCountMap.clear();
        sdHistoryMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        consistentSDCountMap.remove(uuid);
        sdHistoryMap.remove(uuid);
    }

    private Component createComponent(double averageSDDifference) {
        return Component.text()
                .append(Component.text("Average SD Difference: ", NamedTextColor.GRAY))
                .append(Component.text(averageSDDifference + "ms", NamedTextColor.GOLD))
                .build();
    }
}
