package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.server.DamageFeedbackSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for combat-related calculations and operations in the cultivation system.
 *
 * This util class contains shared business logic for damage conversion, immunity tracking,
 * feedback effects, and other combat-related operations. It integrates with the cultivation
 * system to provide authentic Xianxia combat mechanics while maintaining compatibility
 * with vanilla Minecraft and other mods.
 *
 * Key Responsibilities:
 * - Convert vanilla damage/healing to custom cultivation values
 * - Calculate immunity durations and manage immunity tracking
 * - Generate unique damage source identifiers
 * - Apply damage feedback effects (visual, audio, knockback)
 * - Provide combat utility methods for cultivation mechanics
 *
 * Architecture:
 * - Util Pattern: Contains shared business logic for combat operations
 * - Integrates with CombatEventHandler through public static methods
 * - Uses generic methods for different damage/healing types
 * - Follows DRY principles for combat calculations
 */
public class CombatCalculationUtils {

    // === CONSTANTS === //
    private static final long BASE_IMMUNITY_DURATION_MS = 500L;
    private static final long MAX_IMMUNITY_SCALING_MS = 1000L;
    private static final double IMMUNITY_DAMAGE_SCALING = 10.0;
    private static final int DEFAULT_HURT_TIME = 10;
    private static final double DEFAULT_KNOCKBACK_STRENGTH = 0.4;

    // === TRACKING MAPS === //
    private static final Map<UUID, Map<String, Long>> playerImmunityMap = new ConcurrentHashMap<>();
    private static final Map<String, DamageType> damageTypeCache = new ConcurrentHashMap<>();

    // === CONVERSION METHODS === //

    /**
     * Converts vanilla damage to custom cultivation damage using configurable scaling.
     * This method provides the bridge between vanilla Minecraft damage and the cultivation
     * system, maintaining compatibility while allowing for cultivation-specific mechanics.
     *
     * @param vanillaDamage The original vanilla damage amount
     * @param player The player taking damage (used for cultivation-specific scaling)
     * @return Custom damage amount for the cultivation system
     */
    public static long convertToCustomDamage(float vanillaDamage, Player player) {
        if (vanillaDamage <= 0.0f) {
            return 0L;
        }

        try {
            // Get cultivation data for potential scaling
            PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());

            // Currently using 1:1 conversion for vanilla/mod compatibility
            // This can be expanded later for cultivation-specific scaling
            long customDamage = Math.round(vanillaDamage);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Converted damage for {}: {} vanilla -> {} custom",
                    player.getName().getString(), vanillaDamage, customDamage);
            }

            return customDamage;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error converting damage for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return Math.round(vanillaDamage); // Fallback to simple conversion
        }
    }

    /**
     * Converts vanilla healing to custom cultivation healing using configurable scaling.
     * This method handles healing effects from potions, regeneration, and other sources.
     *
     * @param vanillaHealing The original vanilla healing amount
     * @param player The player being healed (used for cultivation-specific scaling)
     * @return Custom healing amount for the cultivation system
     */
    public static long convertToCustomHealing(float vanillaHealing, Player player) {
        if (vanillaHealing <= 0.0f) {
            return 0L;
        }

        try {
            // Get cultivation data for potential scaling
            PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());

            // Simple 1:1 conversion - removed the problematic *100 multiplier
            long customHealing = Math.round(vanillaHealing);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Converted healing for {}: {} vanilla -> {} custom",
                    player.getName().getString(), vanillaHealing, customHealing);
            }

            return customHealing;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error converting healing for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return Math.round(vanillaHealing); // Fallback to simple conversion
        }
    }

    /**
     * Generic method to convert between damage/healing types based on operation type.
     * This follows the same pattern as the AttributeModifierUtils for handling different types.
     *
     * @param <T> The input type (Float for vanilla, Long for custom)
     * @param <R> The return type (Long for custom, Float for vanilla)
     * @param value The value to convert
     * @param player The player for context
     * @param conversionType The type of conversion to perform
     * @return The converted value
     */
    public static <T extends Number, R extends Number> R convertValue(T value, Player player, ConversionType conversionType) {
        try {
            return switch (conversionType) {
                case VANILLA_TO_CUSTOM_DAMAGE -> (R) Long.valueOf(convertToCustomDamage(value.floatValue(), player));
                case VANILLA_TO_CUSTOM_HEALING -> (R) Long.valueOf(convertToCustomHealing(value.floatValue(), player));
                case CUSTOM_TO_VANILLA_DAMAGE -> (R) Float.valueOf(value.longValue()); // Simple reverse conversion
                case CUSTOM_TO_VANILLA_HEALING -> (R) Float.valueOf(value.longValue()); // Simple reverse conversion
            };
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error in generic conversion: {}", e.getMessage(), e);
            return (R) value; // Fallback to original value
        }
    }

    // === IMMUNITY MANAGEMENT === //

    /**
     * Calculates immunity duration based on damage amount and damage type.
     * Higher damage amounts result in longer immunity periods to prevent rapid death.
     *
     * @param damage The damage amount taken
     * @param damageSource The source of damage (optional, for type-specific scaling)
     * @return Immunity duration in milliseconds
     */
    public static long calculateImmunityDuration(long damage, DamageSource damageSource) {
        try {
            // Base immunity with damage scaling
            long baseDuration = BASE_IMMUNITY_DURATION_MS;
            long damageScaling = Math.min((long)(damage / IMMUNITY_DAMAGE_SCALING), MAX_IMMUNITY_SCALING_MS);

            // Additional scaling based on damage type (future expansion)
            double typeMultiplier = getDamageTypeMultiplier(damageSource);

            long totalDuration = (long)((baseDuration + damageScaling) * typeMultiplier);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Calculated immunity duration: {}ms (damage: {}, type: {})",
                    totalDuration, damage, damageSource.type().msgId());
            }

            return totalDuration;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error calculating immunity duration: {}", e.getMessage(), e);
            return BASE_IMMUNITY_DURATION_MS; // Fallback to base duration
        }
    }

    /**
     * Overloaded method for simple immunity calculation without damage source.
     *
     * @param damage The damage amount taken
     * @return Immunity duration in milliseconds
     */
    public static long calculateImmunityDuration(long damage) {
        return calculateImmunityDuration(damage, null);
    }

    /**
     * Sets immunity for a player against a specific damage source.
     * This prevents rapid consecutive damage from the same source.
     *
     * @param playerId The player's UUID
     * @param sourceKey The unique damage source key
     * @param durationMs The immunity duration in milliseconds
     */
    public static void setPlayerImmunity(UUID playerId, String sourceKey, long durationMs) {
        try {
            playerImmunityMap.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(sourceKey, System.currentTimeMillis() + durationMs);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Set immunity for player {} against {}: {}ms",
                    playerId, sourceKey, durationMs);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error setting player immunity: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if a player is immune to a specific damage source.
     *
     * @param playerId The player's UUID
     * @param sourceKey The unique damage source key
     * @return true if the player is currently immune to this damage source
     */
    public static boolean isPlayerImmune(UUID playerId, String sourceKey) {
        try {
            Map<String, Long> playerImmunities = playerImmunityMap.get(playerId);
            if (playerImmunities == null) {
                return false;
            }

            Long immunityEnd = playerImmunities.get(sourceKey);
            if (immunityEnd == null) {
                return false;
            }

            boolean isImmune = System.currentTimeMillis() < immunityEnd;

            // Clean up expired immunity
            if (!isImmune) {
                playerImmunities.remove(sourceKey);
                if (playerImmunities.isEmpty()) {
                    playerImmunityMap.remove(playerId);
                }
            }

            return isImmune;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error checking player immunity: {}", e.getMessage(), e);
            return false; // Assume not immune on error
        }
    }

    /**
     * Clears all immunity for a specific player.
     * Used during respawn or other state resets.
     *
     * @param playerId The player's UUID
     */
    public static void clearPlayerImmunity(UUID playerId) {
        try {
            Map<String, Long> removed = playerImmunityMap.remove(playerId);

            if (removed != null && ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Cleared {} immunity entries for player {}",
                    removed.size(), playerId);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error clearing player immunity: {}", e.getMessage(), e);
        }
    }

    // === DAMAGE SOURCE UTILITIES === //

    /**
     * Generates a unique key for a damage source to track immunity separately per source.
     * This allows players to be immune to one enemy while still taking damage from others.
     *
     * @param damageSource The damage source to generate a key for
     * @return A unique string identifier for this damage source
     */
    public static String generateDamageSourceKey(DamageSource damageSource) {
        try {
            if (damageSource == null) {
                return "unknown_damage";
            }

            String baseKey = damageSource.type().msgId();

            // Include entity UUID for entity-based damage sources
            Entity sourceEntity = damageSource.getEntity();
            if (sourceEntity != null) {
                return baseKey + "_" + sourceEntity.getUUID();
            }

            // Include direct entity UUID for direct damage
            Entity directEntity = damageSource.getDirectEntity();
            if (directEntity != null) {
                return baseKey + "_direct_" + directEntity.getUUID();
            }

            return baseKey;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error generating damage source key: {}", e.getMessage(), e);
            return "error_damage_" + System.currentTimeMillis(); // Unique fallback
        }
    }

    // === FEEDBACK EFFECTS === //

    /**
     * Applies damage feedback effects including hurt animation, knockback, and client sync.
     * This provides visual and mechanical feedback for custom damage system.
     *
     * @param player The player taking damage
     * @param damage The actual damage amount dealt
     * @param remainingHealth The player's remaining health after damage
     * @param damageSource The source of the damage
     */
    public static void applyDamageFeedback(Player player, long damage, long remainingHealth, DamageSource damageSource) {
        try {
            // Apply hurt animation
            player.hurtTime = DEFAULT_HURT_TIME;
            player.hurtDuration = DEFAULT_HURT_TIME;

            // Apply knockback if appropriate
            applyDamageKnockback(player, damageSource);

            // Send feedback packet to client for visual effects
            if (player instanceof ServerPlayer serverPlayer) {
                sendDamageFeedbackToClient(serverPlayer, damage, damageSource);
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Applied damage feedback for {}: {} damage, {} health remaining",
                    player.getName().getString(), damage, remainingHealth);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error applying damage feedback for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Applies knockback effects based on damage source and amount.
     *
     * @param player The player to apply knockback to
     * @param damageSource The source of damage
     */
    private static void applyDamageKnockback(Player player, DamageSource damageSource) {
        try {
            Entity sourceEntity = damageSource.getEntity();
            if (sourceEntity == null) {
                return;
            }

            // Calculate knockback direction
            Vec3 playerPos = player.position();
            Vec3 sourcePos = sourceEntity.position();
            Vec3 knockbackDirection = playerPos.subtract(sourcePos).normalize();

            // Apply knockback
            Vec3 knockback = knockbackDirection.scale(DEFAULT_KNOCKBACK_STRENGTH);
            player.setDeltaMovement(player.getDeltaMovement().add(knockback));

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[CombatCalculationUtils] Applied knockback to {}: {}",
                    player.getName().getString(), knockback);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error applying knockback: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends damage feedback information to the client for visual effects.
     *
     * @param player The server player to send the packet to
     * @param damage The damage amount
     * @param damageSource The damage source
     */
    private static void sendDamageFeedbackToClient(ServerPlayer player, long damage, DamageSource damageSource) {
        try {
            String sourceId = generateDamageSourceKey(damageSource);
            DamageFeedbackSyncPacket packet = DamageFeedbackSyncPacket.create(damage, sourceId);
            ICModPacketRegistry.sendToPlayer(packet, player);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error sending damage feedback packet: {}", e.getMessage(), e);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Gets a damage type multiplier for immunity calculation.
     * This can be expanded to provide different immunity durations for different damage types.
     *
     * @param damageSource The damage source
     * @return A multiplier for immunity duration (1.0 = default)
     */
    private static double getDamageTypeMultiplier(DamageSource damageSource) {
        if (damageSource == null) {
            return 1.0;
        }

        try {
            String damageTypeId = damageSource.type().msgId();
            DamageType cachedType = damageTypeCache.computeIfAbsent(damageTypeId, key ->
                DamageType.fromDamageSource(damageSource));

            return cachedType.getImmunityMultiplier();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[CombatCalculationUtils] Error getting damage type multiplier: {}", e.getMessage(), e);
            return 1.0;
        }
    }

    // === ENUMS AND INNER CLASSES === //

    /**
     * Enum for different conversion types in the generic conversion method.
     */
    public enum ConversionType {
        VANILLA_TO_CUSTOM_DAMAGE,
        VANILLA_TO_CUSTOM_HEALING,
        CUSTOM_TO_VANILLA_DAMAGE,
        CUSTOM_TO_VANILLA_HEALING
    }

    /**
     * Inner class representing different damage types and their properties.
     */
    private static class DamageType {
        private final String id;
        private final double immunityMultiplier;

        private DamageType(String id, double immunityMultiplier) {
            this.id = id;
            this.immunityMultiplier = immunityMultiplier;
        }

        public static DamageType fromDamageSource(DamageSource damageSource) {
            String id = damageSource.type().msgId();

            // Define different immunity multipliers for different damage types
            double multiplier = switch (id) {
                case "arrow", "trident" -> 0.8; // Projectiles have shorter immunity
                case "explosion", "fireball" -> 1.5; // Explosions have longer immunity
                case "magic", "thorns" -> 1.2; // Magic damage has slightly longer immunity
                case "fall", "flyIntoWall" -> 0.6; // Environmental damage has shorter immunity
                default -> 1.0; // Default multiplier
            };

            return new DamageType(id, multiplier);
        }

        public double getImmunityMultiplier() {
            return immunityMultiplier;
        }

        public String getId() {
            return id;
        }
    }
}