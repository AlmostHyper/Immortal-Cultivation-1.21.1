package net.hyper.immortalcultivation.server.events.data;

import net.hyper.immortalcultivation.ImmortalCultivation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combat Data Manager for the Immortal Cultivation Mod
 *
 * Manages combat-related data storage following SOLID principles.
 * Responsible for tracking player immunity, health states, and combat-related caching.
 * Separates data concerns from event handling logic.
 *
 * Key Features:
 * - Player immunity tracking for sophisticated multi-entity combat
 * - Vanilla health state management for protection system compatibility
 * - Automatic cleanup of expired data for memory efficiency
 * - Thread-safe operations with concurrent data structures
 *
 * Architecture:
 * - Singleton pattern for global access
 * - Manager pattern for data coordination
 * - Performance optimized with automatic cleanup
 */
public class CombatEventData {

    // === SINGLETON INSTANCE === //
    private static CombatEventData instance;

    // === CONSTANTS === //
    private static final String MANAGER_NAME = "CombatDataManager";
    private static final int IMMUNITY_CLEANUP_INTERVAL = 6000; // Cleanup every 5 minutes

    // === DATA STORAGE === //

    /**
     * Tracks immunity per player per damage source for sophisticated multi-enemy combat.
     * Key: Player UUID, Value: Map of damage source keys to immunity expiration times
     */
    private final Map<UUID, Map<String, Long>> playerImmunityMap = new ConcurrentHashMap<>();

    /**
     * Tracks last known vanilla health values for protection system compatibility.
     * Key: Player UUID, Value: Last recorded vanilla health
     */
    private final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();

    /**
     * Tracks last immunity cleanup time to prevent excessive cleanup operations
     */
    private long lastCleanupTime = 0L;

    // === CONSTRUCTOR === //

    /**
     * Private constructor for singleton pattern
     */
    private CombatEventData() {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Instance created", MANAGER_NAME);
        }
    }

    // === SINGLETON ACCESS === //

    /**
     * Gets the singleton instance of CombatDataManager
     *
     * @return The CombatDataManager instance
     */
    public static CombatEventData getInstance() {
        if (instance == null) {
            synchronized (CombatEventData.class) {
                if (instance == null) {
                    instance = new CombatEventData();
                }
            }
        }
        return instance;
    }

    // === IMMUNITY MANAGEMENT === //

    /**
     * Alias for hasPlayerImmunity to maintain compatibility with refactored utils
     *
     * @param playerId The player's UUID
     * @param damageSourceKey The damage source identifier
     * @return True if the player is currently immune
     */
    public boolean isPlayerImmune(UUID playerId, String damageSourceKey) {
        return hasPlayerImmunity(playerId, damageSourceKey);
    }

    /**
     * Sets immunity for a player against a specific damage source using duration.
     * Overloaded method to accept duration in milliseconds from current time.
     *
     * @param playerId The player's UUID
     * @param damageSourceKey The damage source identifier
     * @param durationMs The immunity duration in milliseconds from now
     */
    public void setPlayerImmunity(UUID playerId, String damageSourceKey, long durationMs) {
        setPlayerImmunity(playerId, damageSourceKey, System.currentTimeMillis() + durationMs);
    }

    /**
     * Checks if a player has immunity against a specific damage source
     *
     * @param playerId The player's UUID
     * @param damageSourceKey The damage source identifier
     * @return True if the player is currently immune
     */
    public boolean hasPlayerImmunity(UUID playerId, String damageSourceKey) {
        Map<String, Long> playerImmunities = playerImmunityMap.get(playerId);
        if (playerImmunities == null) {
            return false;
        }

        Long immunityEndTime = playerImmunities.get(damageSourceKey);
        if (immunityEndTime == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= immunityEndTime) {
            // Immunity expired, remove it
            playerImmunities.remove(damageSourceKey);
            if (playerImmunities.isEmpty()) {
                playerImmunityMap.remove(playerId);
            }
            return false;
        }

        return true;
    }

    /**
     * Removes all immunity data for a specific player
     *
     * @param playerId The player's UUID
     */
    public void clearPlayerImmunity(UUID playerId) {
        playerImmunityMap.remove(playerId);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared all immunity for player {}", MANAGER_NAME, playerId);
        }
    }

    // === VANILLA HEALTH MANAGEMENT === //

    /**
     * Updates the last known vanilla health for a player
     *
     * @param playerId The player's UUID
     * @param health The vanilla health value
     */
    public void updateLastKnownVanillaHealth(UUID playerId, float health) {
        lastKnownVanillaHealth.put(playerId, health);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Updated vanilla health for player {} to {}",
                MANAGER_NAME, playerId, health);
        }
    }

    /**
     * Gets the last known vanilla health for a player
     *
     * @param playerId The player's UUID
     * @return The last known vanilla health, or null if not tracked
     */
    @Nullable
    public Float getLastKnownVanillaHealth(UUID playerId) {
        return lastKnownVanillaHealth.get(playerId);
    }

    /**
     * Removes vanilla health tracking for a specific player
     *
     * @param playerId The player's UUID
     */
    public void clearLastKnownVanillaHealth(UUID playerId) {
        lastKnownVanillaHealth.remove(playerId);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared vanilla health tracking for player {}", MANAGER_NAME, playerId);
        }
    }

    // === CLEANUP OPERATIONS === //

    /**
     * Performs cleanup of expired immunity data if enough time has passed
     * Called automatically during immunity checks, but can be called manually
     */
    public void performCleanupIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime >= IMMUNITY_CLEANUP_INTERVAL) {
            performCleanup();
            lastCleanupTime = currentTime;
        }
    }

    /**
     * Forces an immediate cleanup of all expired immunity data
     */
    public void performCleanup() {
        long currentTime = System.currentTimeMillis();
        final int[] removedEntries = {0};

        playerImmunityMap.entrySet().removeIf(playerEntry -> {
            UUID playerId = playerEntry.getKey();
            Map<String, Long> immunities = playerEntry.getValue();

            // Remove expired immunities for this player
            immunities.entrySet().removeIf(immunityEntry -> {
                boolean expired = currentTime >= immunityEntry.getValue();
                if (expired) {
                    removedEntries[0]++;
                }
                return expired;
            });

            // Remove player entry if no immunities remain
            return immunities.isEmpty();
        });

        if (ImmortalCultivation.LOGGER.isDebugEnabled() && removedEntries[0] > 0) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleanup removed {} expired immunity entries",
                MANAGER_NAME, removedEntries[0]);
        }
    }

    /**
     * Clears all data for a specific player (typically called on logout)
     *
     * @param playerId The player's UUID
     */
    public void clearAllPlayerData(UUID playerId) {
        clearPlayerImmunity(playerId);
        clearLastKnownVanillaHealth(playerId);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared all data for player {}", MANAGER_NAME, playerId);
        }
    }

    /**
     * Gets the total number of tracked immunity entries across all players
     *
     * @return Total immunity entries count
     */
    public int getTotalImmunityEntries() {
        return playerImmunityMap.values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    /**
     * Gets the number of players being tracked for vanilla health
     *
     * @return Number of tracked players
     */
    public int getTrackedVanillaHealthPlayers() {
        return lastKnownVanillaHealth.size();
    }
}