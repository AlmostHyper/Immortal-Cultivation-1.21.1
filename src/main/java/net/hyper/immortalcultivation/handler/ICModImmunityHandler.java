package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PER-SOURCE IMMUNITY SYSTEM
 *
 * This handler works alongside ICModHealthHandler to provide sophisticated immunity logic.
 * It replaces vanilla's global immunity with per-source immunity to enable multi-enemy combat
 * while still preventing environmental damage spam.
 *
 * Integration: This runs BEFORE ICModHealthHandler to determine if damage should be blocked.
 * If damage is blocked here, ICModHealthHandler never sees it.
 */
public class ICModImmunityHandler {

    // === IMMUNITY TRACKING ===

    /**
     * Tracks immunity per player per damage source
     * Map<PlayerUUID, Map<DamageSourceKey, LastDamageTime>>
     */
    private static final Map<UUID, Map<String, Long>> playerSourceImmunity = new ConcurrentHashMap<>();

    // === IMMUNITY CONFIGURATION ===

    // Combat damage immunity (per entity) - allows multiple enemies to attack
    private static final long ENTITY_DAMAGE_IMMUNITY_TICKS = 10; // 0.5 seconds between hits from same entity

    // Environmental damage immunity (per source type) - prevents spam
    private static final long ENVIRONMENTAL_DAMAGE_IMMUNITY_TICKS = 20; // 1 second for environmental sources

    // Special immunity for rapid environmental sources (berry bushes, cactus)
    private static final long RAPID_ENVIRONMENTAL_IMMUNITY_TICKS = 40; // 2 seconds for rapid sources

    // Global minimum damage interval (prevents absurd damage rates)
    private static final long GLOBAL_MIN_DAMAGE_INTERVAL = 2; // 0.1 seconds minimum between ANY damage

    // === MAIN IMMUNITY CHECK ===

    /**
     * PRIMARY IMMUNITY CHECK - Runs before ICModHealthHandler
     *
     * This determines if damage should be blocked entirely. If blocked here,
     * ICModHealthHandler will never see the damage event.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void checkImmunity(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        // If player is immune to this specific damage source, block it completely
        if (isImmuneToSource(player, event.getSource())) {
            ImmortalCultivation.LOGGER.debug("Player {} is immune to damage from source: {} - blocking damage",
                player.getName().getString(), event.getSource().getMsgId());

            // Completely cancel the damage event - ICModHealthHandler won't see it
            event.setCanceled(true);
            return;
        }

        // If damage is allowed, set immunity for this source and let ICModHealthHandler handle it
        setSourceImmunity(player, event.getSource());

        ImmortalCultivation.LOGGER.debug("Player {} can take damage from source: {} - setting immunity",
            player.getName().getString(), event.getSource().getMsgId());
    }

    // === IMMUNITY LOGIC ===

    /**
     * Check if player is immune to damage from this specific source
     */
    private static boolean isImmuneToSource(Player player, DamageSource damageSource) {
        UUID playerId = player.getUUID();
        Map<String, Long> sourceImmunity = playerSourceImmunity.get(playerId);

        if (sourceImmunity == null) {
            return false;
        }

        String sourceKey = createSourceKey(damageSource);
        Long lastDamageTime = sourceImmunity.get(sourceKey);

        if (lastDamageTime == null) {
            return false;
        }

        long currentTime = player.level().getGameTime();
        long timeSinceLastDamage = currentTime - lastDamageTime;

        // Determine immunity duration based on damage source type
        long immunityDuration = getImmunityDuration(damageSource);

        boolean isImmune = timeSinceLastDamage < immunityDuration;

        if (isImmune) {
            ImmortalCultivation.LOGGER.debug("Source {} immune for {} more ticks",
                sourceKey, immunityDuration - timeSinceLastDamage);
        }

        return isImmune;
    }

    /**
     * Set immunity for this damage source
     */
    private static void setSourceImmunity(Player player, DamageSource damageSource) {
        UUID playerId = player.getUUID();
        Map<String, Long> sourceImmunity = playerSourceImmunity.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        String sourceKey = createSourceKey(damageSource);
        long currentTime = player.level().getGameTime();

        sourceImmunity.put(sourceKey, currentTime);

        ImmortalCultivation.LOGGER.debug("Set immunity for player {} from source {} at time {}",
            player.getName().getString(), sourceKey, currentTime);

        // IMPORTANT: We don't set player.invulnerableTime here!
        // That's handled by ICModHealthHandler for visual feedback only
    }

    /**
     * Create a unique key for this damage source
     *
     * This is the core of the per-source immunity system:
     * - Different entities get different keys (allows multi-enemy combat)
     * - Same environmental sources share keys (prevents spam)
     */
    private static String createSourceKey(DamageSource damageSource) {
        StringBuilder keyBuilder = new StringBuilder();

        // Base damage type
        keyBuilder.append(damageSource.getMsgId());

        // If there's a direct entity (like a specific zombie), include its UUID
        if (damageSource.getDirectEntity() != null && !isEnvironmentalDamage(damageSource)) {
            keyBuilder.append("_entity_").append(damageSource.getDirectEntity().getUUID());
        }
        // For environmental damage, group by type (all lava blocks share immunity)
        else if (isEnvironmentalDamage(damageSource)) {
            keyBuilder.append("_environmental");
        }
        // If there's an indirect entity (like the thrower of a projectile), include it
        else if (damageSource.getEntity() != null) {
            keyBuilder.append("_indirect_").append(damageSource.getEntity().getUUID());
        }

        return keyBuilder.toString();
    }

    /**
     * Get immunity duration for this damage source type
     */
    private static long getImmunityDuration(DamageSource damageSource) {
        if (isRapidEnvironmentalDamage(damageSource)) {
            return RAPID_ENVIRONMENTAL_IMMUNITY_TICKS;
        } else if (isEnvironmentalDamage(damageSource)) {
            return ENVIRONMENTAL_DAMAGE_IMMUNITY_TICKS;
        } else {
            return ENTITY_DAMAGE_IMMUNITY_TICKS;
        }
    }

    /**
     * Check if this is environmental damage that needs immunity protection
     */
    private static boolean isEnvironmentalDamage(DamageSource damageSource) {
        return damageSource.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
            damageSource.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING) ||
            damageSource.getMsgId().equals("lava") ||
            damageSource.getMsgId().equals("inFire") ||
            damageSource.getMsgId().equals("onFire") ||
            damageSource.getMsgId().equals("hotFloor") ||
            damageSource.getMsgId().equals("magma") ||
            damageSource.getMsgId().equals("sweetBerryBush") ||
            damageSource.getMsgId().equals("cactus") ||
            damageSource.getMsgId().equals("inWall") ||
            damageSource.getMsgId().equals("cramming");
    }

    /**
     * Check if this is rapid environmental damage that needs extra protection
     */
    private static boolean isRapidEnvironmentalDamage(DamageSource damageSource) {
        return damageSource.getMsgId().equals("sweetBerryBush") ||
            damageSource.getMsgId().equals("cactus") ||
            damageSource.getMsgId().equals("inWall") ||
            damageSource.getMsgId().equals("cramming");
    }

    // === CLEANUP AND MAINTENANCE ===

    /**
     * Clean up old immunity entries to prevent memory leaks
     */
    @SubscribeEvent
    public static void cleanupImmunity(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        // Clean up old immunity entries every 5 seconds
        if (player.tickCount % 100 == 0) {
            UUID playerId = player.getUUID();
            cleanupOldImmunity(playerId, player.level().getGameTime());
        }
    }

    /**
     * Clean up old immunity entries to prevent memory leaks
     */
    private static void cleanupOldImmunity(UUID playerId, long currentTime) {
        Map<String, Long> sourceImmunity = playerSourceImmunity.get(playerId);
        if (sourceImmunity != null) {
            sourceImmunity.entrySet().removeIf(entry -> {
                long timeSinceLastDamage = currentTime - entry.getValue();
                // Clean up entries older than double the max immunity time
                return timeSinceLastDamage > RAPID_ENVIRONMENTAL_IMMUNITY_TICKS * 2;
            });

            // Remove empty maps
            if (sourceImmunity.isEmpty()) {
                playerSourceImmunity.remove(playerId);
            }
        }
    }

    // === PLAYER LIFECYCLE ===

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Initialize immunity tracking for new player
        UUID playerId = event.getEntity().getUUID();
        playerSourceImmunity.put(playerId, new ConcurrentHashMap<>());

        ImmortalCultivation.LOGGER.info("Initialized immunity tracking for player {}",
            event.getEntity().getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Clear all immunity on respawn
        UUID playerId = event.getEntity().getUUID();
        playerSourceImmunity.put(playerId, new ConcurrentHashMap<>());

        ImmortalCultivation.LOGGER.info("Reset immunity tracking for respawned player {}",
            event.getEntity().getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up on logout
        UUID playerId = event.getEntity().getUUID();
        playerSourceImmunity.remove(playerId);

        ImmortalCultivation.LOGGER.debug("Cleaned up immunity tracking for player {}",
            event.getEntity().getName().getString());
    }

    // === PUBLIC DEBUG/ADMIN METHODS ===

    /**
     * Clear all immunity for a player (admin/debug use)
     */
    public static void clearPlayerImmunity(Player player) {
        UUID playerId = player.getUUID();
        playerSourceImmunity.remove(playerId);
        ImmortalCultivation.LOGGER.info("Cleared all immunity for player {}", player.getName().getString());
    }

    /**
     * Get immunity status for a player (debug use)
     */
    public static void debugPlayerImmunity(Player player) {
        UUID playerId = player.getUUID();
        Map<String, Long> sourceImmunity = playerSourceImmunity.get(playerId);
        long currentTime = player.level().getGameTime();

        ImmortalCultivation.LOGGER.info("=== IMMUNITY DEBUG FOR {} ===", player.getName().getString());

        if (sourceImmunity == null || sourceImmunity.isEmpty()) {
            ImmortalCultivation.LOGGER.info("No active source immunities");
        } else {
            for (Map.Entry<String, Long> entry : sourceImmunity.entrySet()) {
                long timeSince = currentTime - entry.getValue();
                long immunityDuration = getImmunityDurationForKey(entry.getKey());
                long remaining = Math.max(0, immunityDuration - timeSince);

                ImmortalCultivation.LOGGER.info("Source: {} - {} ticks remaining",
                    entry.getKey(), remaining);
            }
        }
        ImmortalCultivation.LOGGER.info("=== END IMMUNITY DEBUG ===");
    }

    /**
     * Helper method to get immunity duration for a source key (for debug)
     */
    private static long getImmunityDurationForKey(String sourceKey) {
        if (sourceKey.contains("sweetBerryBush") || sourceKey.contains("cactus") ||
            sourceKey.contains("inWall") || sourceKey.contains("cramming")) {
            return RAPID_ENVIRONMENTAL_IMMUNITY_TICKS;
        } else if (sourceKey.contains("_environmental")) {
            return ENVIRONMENTAL_DAMAGE_IMMUNITY_TICKS;
        } else {
            return ENTITY_DAMAGE_IMMUNITY_TICKS;
        }
    }

    /**
     * Check if a player can currently take damage from a specific source (public API)
     */
    public static boolean canTakeDamageFromSource(Player player, DamageSource damageSource) {
        return !isImmuneToSource(player, damageSource);
    }

    /**
     * Force set immunity for a source (for special abilities/items)
     */
    public static void forceSetImmunity(Player player, DamageSource damageSource, long durationTicks) {
        UUID playerId = player.getUUID();
        Map<String, Long> sourceImmunity = playerSourceImmunity.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        String sourceKey = createSourceKey(damageSource);
        long immunityUntil = player.level().getGameTime() + durationTicks;

        sourceImmunity.put(sourceKey, immunityUntil);

        ImmortalCultivation.LOGGER.info("Forced immunity for player {} from source {} for {} ticks",
            player.getName().getString(), sourceKey, durationTicks);
    }
}