package com.deathmotion.totemguard.database;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.data.CheckDetails;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.database.entities.Check;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.ebean.Database;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import org.jetbrains.annotations.Blocking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseService {
    private static final int SAVE_INTERVAL_SECONDS = 10;
    private static final int CACHE_EXPIRY_MINUTES = 10;
    private static final String UUID_FIELD = "uuid";
    private static final String WHEN_CREATED_FIELD = "whenCreated";
    private static final String DATABASE_PLAYER_FIELD = "databasePlayer";

    private final TotemGuard plugin;
    private final Database database;
    private final ZoneId zoneId;
    private final BlockingQueue<Alert> alertsToSave = new LinkedBlockingQueue<>();
    private final BlockingQueue<Punishment> punishmentsToSave = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<UUID, CacheEntry> playerCache = new ConcurrentHashMap<>();

    public DatabaseService(TotemGuard plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager().getDatabase();
        this.zoneId = ZoneId.systemDefault();

        // Schedule async tasks
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o -> bulkSave()), SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o -> cleanupCache()), CACHE_EXPIRY_MINUTES, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Periodically saves all queued alerts and punishments to the database.
     */
    private void bulkSave() {
        List<Alert> alertsSnapshot = new ArrayList<>();
        List<Punishment> punishmentsSnapshot = new ArrayList<>();

        // Drain the queues into the snapshots
        alertsToSave.drainTo(alertsSnapshot);
        punishmentsToSave.drainTo(punishmentsSnapshot);

        try {
            if (!alertsSnapshot.isEmpty()) {
                database.saveAll(alertsSnapshot);
            }
            if (!punishmentsSnapshot.isEmpty()) {
                database.saveAll(punishmentsSnapshot);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save data to the database: " + e.getMessage());
            // Ensure atomic re-adding
            synchronized (this) {
                alertsToSave.addAll(alertsSnapshot);
                punishmentsToSave.addAll(punishmentsSnapshot);
            }
        }
    }

    /**
     * Saves an alert to the queue for asynchronous saving.
     */
    public void saveAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            Alert alert = createAlert(totemPlayer, checkDetails);
            if (!alertsToSave.offer(alert)) {
                plugin.getLogger().severe("Failed to enqueue alert for player: " + totemPlayer.getUsername());
            }
        });
    }

    /**
     * Saves a punishment to the queue for asynchronous saving.
     */
    public void savePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            Punishment punishment = createPunishment(totemPlayer, checkDetails);
            if (!punishmentsToSave.offer(punishment)) {
                plugin.getLogger().severe("Failed to enqueue punishment for player: " + totemPlayer.getUsername());
            }
        });
    }

    /**
     * Retrieves the DatabasePlayer by UUID, using a cache with a 10-minute expiration.
     */
    private DatabasePlayer getOrCreatePlayer(UUID uuid) {
        // Check cache first
        CacheEntry cacheEntry = playerCache.get(uuid);
        if (cacheEntry != null && !cacheEntry.isExpired()) {
            return cacheEntry.getDatabasePlayer();
        }

        // If not in cache, retrieve or create from database
        DatabasePlayer databasePlayer = database.find(DatabasePlayer.class).where().eq(UUID_FIELD, uuid).findOneOrEmpty()
                .orElseGet(() -> {
                    DatabasePlayer newPlayer = new DatabasePlayer();
                    newPlayer.setUuid(uuid);
                    try {
                        newPlayer.save();
                    } catch (Exception e) {
                        // Handle case where another thread inserted the player concurrently
                        return database.find(DatabasePlayer.class).where().eq(UUID_FIELD, uuid).findOne();
                    }
                    return newPlayer;
                });

        // Cache the result
        playerCache.put(uuid, new CacheEntry(databasePlayer));
        return databasePlayer;
    }

    /**
     * Cleans up expired entries from the cache.
     */
    private void cleanupCache() {
        playerCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private Alert createAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(totemPlayer.getUuid());
        Alert alert = new Alert();
        alert.setCheckName(Check.valueOf(checkDetails.getCheckName()));
        alert.setDatabasePlayer(databasePlayer);

        // Add alert to player's list to maintain bidirectional relationship
        databasePlayer.getAlerts().add(alert);
        return alert;
    }

    private Punishment createPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(totemPlayer.getUuid());
        Punishment punishment = new Punishment();
        punishment.setCheckName(Check.valueOf(checkDetails.getCheckName()));
        punishment.setDatabasePlayer(databasePlayer);

        // Add punishment to player's list to maintain bidirectional relationship
        databasePlayer.getPunishments().add(punishment);
        return punishment;
    }

    /**
     * Retrieves all alerts from the database.
     */
    @Blocking
    public List<Alert> getAlerts() {
        return database.find(Alert.class).findList();
    }

    /**
     * Retrieves all punishments from the database.
     */
    @Blocking
    public List<Punishment> getPunishments() {
        return database.find(Punishment.class).findList();
    }

    /**
     * Retrieves the logs for the given player, consisting of two lists: alerts and punishments.
     */
    @Blocking
    public Pair<List<Alert>, List<Punishment>> getLogs(UUID uuid) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(uuid);

        List<Alert> alerts = database.find(Alert.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .findList();

        List<Punishment> punishments = database.find(Punishment.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .findList();

        return new Pair<>(alerts, punishments);
    }

    /**
     * Deletes all alerts and punishments for a specific UUID by fetching the related DatabasePlayer.
     */
    @Blocking
    public int clearLogs(UUID uuid) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(uuid);

        // Delete alerts and punishments related to the databasePlayer
        int deletedAlerts = database.find(Alert.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .delete();
        int deletedPunishments = database.find(Punishment.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    /**
     * Trims the database by removing alerts and punishments older than 30 days.
     */
    @Blocking
    public int trimDatabase() {
        Instant thirtyDaysAgo = LocalDateTime.now().minusDays(30).atZone(zoneId).toInstant();

        // Delete alerts older than 30 days
        int deletedAlerts = database.find(Alert.class)
                .where()
                .lt(WHEN_CREATED_FIELD, thirtyDaysAgo)
                .delete();

        // Delete punishments older than 30 days
        int deletedPunishments = database.find(Punishment.class)
                .where()
                .lt(WHEN_CREATED_FIELD, thirtyDaysAgo)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    /**
     * Clears all DatabasePlayers along with their associated alerts and punishments from the database.
     */
    @Blocking
    public int clearDatabase() {
        int totalAlerts = database.find(Alert.class).findCount();
        int totalPunishments = database.find(Punishment.class).findCount();
        int deletedPlayers = database.find(DatabasePlayer.class).delete();

        // Clear the cache
        playerCache.clear();

        // Calculate the total number of deleted records
        return totalAlerts + totalPunishments + deletedPlayers;
    }

    /**
     * Cache entry that contains the DatabasePlayer and its creation timestamp.
     */
    private static class CacheEntry {
        @Getter
        private final DatabasePlayer databasePlayer;
        private final long timestamp;

        public CacheEntry(DatabasePlayer databasePlayer) {
            this.databasePlayer = databasePlayer;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(CACHE_EXPIRY_MINUTES);
        }
    }
}
