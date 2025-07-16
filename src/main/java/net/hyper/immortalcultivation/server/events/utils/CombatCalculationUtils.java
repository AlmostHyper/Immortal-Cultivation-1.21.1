package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.network.NetworkRegistry;
import net.hyper.immortalcultivation.core.network.packets.server.DamageFeedbackSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for combat-related calculations and operations in the cultivation system.
 *
 * This util class contains shared business logic for damage conversion, immunity tracking,
 * feedback effects, and other combat-related operations. It integrates with the cultivation
 * system to provide authentic Xianxia combat mechanics while maintaining compatibility
 * with vanilla Minecraft and other mods.
 *
 * Refactored to be stateless and work with CombatEventData for data management.
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
 * - Stateless design - no internal data storage
 * - Integrates with CombatEventHandler through public static methods
 * - Uses generic methods for different damage/healing types
 * - Follows DRY principles for combat calculations
 */
public class CombatCalculationUtils {

    // === CONSTANTS === //
    private static final String UTILS_NAME = "CombatCalculationUtils";
    private static final long BASE_IMMUNITY_DURATION_MS = 500L;
    private static final long MAX_IMMUNITY_SCALING_MS = 1000L;
    private static final double IMMUNITY_DAMAGE_SCALING = 10.0;
    private static final int DEFAULT_HURT_TIME = 10;
    private static final double DEFAULT_KNOCKBACK_STRENGTH = 0.4;
    private static final double DEFAULT_DAMAGE_CONVERSION_RATE = 10.0; // 1 vanilla damage = 10 cultivation damage

    // === CACHING === //
    private static final Map<String, DamageType> damageTypeCache = new ConcurrentHashMap<>();

    // === CONVERSION METHODS === //

    /**
     * Converts vanilla damage to custom cultivation damage using configurable scaling.
     * This method provides the bridge between vanilla Minecraft damage and the cultivation
     * system, maintaining compatibility while allowing for cultivation-specific mechanics.
     *
     * @param vanillaDamage The vanilla damage amount
     * @return The equivalent cultivation damage
     */
    public static long convertVanillaToCultivationDamage(float vanillaDamage) {
        try {
            if (vanillaDamage <= 0.0f) {
                return 0L;
            }

            // Convert with scaling factor
            long cultivationDamage = Math.round(vanillaDamage * DEFAULT_DAMAGE_CONVERSION_RATE);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Converted {} vanilla damage to {} cultivation damage",
                    UTILS_NAME, vanillaDamage, cultivationDamage);
            }

            return cultivationDamage;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error converting vanilla damage: {}", UTILS_NAME, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Converts cultivation damage back to vanilla damage for display or compatibility.
     *
     * @param cultivationDamage The cultivation damage amount
     * @return The equivalent vanilla damage
     */
    public static float convertCultivationToVanillaDamage(long cultivationDamage) {
        try {
            if (cultivationDamage <= 0L) {
                return 0.0f;
            }

            float vanillaDamage = (float) cultivationDamage / (float) DEFAULT_DAMAGE_CONVERSION_RATE;

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Converted {} cultivation damage to {} vanilla damage",
                    UTILS_NAME, cultivationDamage, vanillaDamage);
            }

            return vanillaDamage;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error converting cultivation damage: {}", UTILS_NAME, e.getMessage(), e);
            return 0.0f;
        }
    }

    // === IMMUNITY CALCULATION === //

    /**
     * Calculates immunity duration based on damage taken and damage source type.
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
                ImmortalCultivation.LOGGER.debug("[{}] Calculated immunity duration: {}ms (damage: {}, type: {})",
                    UTILS_NAME, totalDuration, damage,
                    damageSource != null ? damageSource.type().msgId() : "unknown");
            }

            return totalDuration;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error calculating immunity duration: {}", UTILS_NAME, e.getMessage(), e);
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

    // === DAMAGE SOURCE IDENTIFICATION === //

    /**
     * Generates a unique key for a damage source for immunity tracking.
     * This ensures different damage sources can have separate immunity timers.
     *
     * @param damageSource The damage source to generate a key for
     * @return A unique string key for the damage source
     */
    public static String generateDamageSourceKey(DamageSource damageSource) {
        try {
            if (damageSource == null) {
                return "unknown_damage";
            }

            StringBuilder keyBuilder = new StringBuilder();

            // Add damage type
            keyBuilder.append(damageSource.type().msgId());

            // Add source entity if present
            Entity sourceEntity = damageSource.getEntity();
            if (sourceEntity != null) {
                keyBuilder.append("_entity_").append(sourceEntity.getUUID());
            }

            // Add direct entity if different from source
            Entity directEntity = damageSource.getDirectEntity();
            if (directEntity != null && directEntity != sourceEntity) {
                keyBuilder.append("_direct_").append(directEntity.getUUID());
            }

            String key = keyBuilder.toString();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Generated damage source key: {}", UTILS_NAME, key);
            }

            return key;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error generating damage source key: {}", UTILS_NAME, e.getMessage(), e);
            return "error_damage_" + System.currentTimeMillis();
        }
    }

    // === FEEDBACK EFFECTS === //

    /**
     * Applies damage feedback effects including hurt time, knockback, and visual effects.
     *
     * @param player The player receiving damage feedback
     * @param damageSource The source of the damage
     * @param damage The amount of damage for scaling effects
     */
    public static void applyDamageFeedback(ServerPlayer player, DamageSource damageSource, long damage) {
        try {
            // Set hurt time for visual feedback
            player.hurtTime = DEFAULT_HURT_TIME;
            player.hurtDuration = DEFAULT_HURT_TIME;

            // Apply knockback if damage source has an entity
            Entity sourceEntity = damageSource.getEntity();
            if (sourceEntity != null) {
                applyKnockback(player, sourceEntity, damage);
            }

            // Send damage feedback packet to client
            sendDamageFeedbackPacket(player, damage);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Applied damage feedback for player {} (damage: {})",
                    UTILS_NAME, player.getName().getString(), damage);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error applying damage feedback: {}", UTILS_NAME, e.getMessage(), e);
        }
    }

    /**
     * Applies knockback effect based on damage amount and source location.
     *
     * @param player The player to apply knockback to
     * @param sourceEntity The entity causing the knockback
     * @param damage The damage amount for scaling knockback strength
     */
    private static void applyKnockback(ServerPlayer player, Entity sourceEntity, long damage) {
        try {
            Vec3 playerPos = player.position();
            Vec3 sourcePos = sourceEntity.position();
            Vec3 direction = playerPos.subtract(sourcePos).normalize();

            // Scale knockback based on damage (with reasonable limits)
            double knockbackStrength = Math.min(DEFAULT_KNOCKBACK_STRENGTH * (1.0 + damage / 100.0), 2.0);

            Vec3 knockback = direction.scale(knockbackStrength);
            player.setDeltaMovement(player.getDeltaMovement().add(knockback));

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Applied knockback {} to player {}",
                    UTILS_NAME, knockbackStrength, player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error applying knockback: {}", UTILS_NAME, e.getMessage(), e);
        }
    }

    /**
     * Sends damage feedback packet to client for visual/audio effects.
     *
     * @param player The player to send feedback to
     * @param damage The damage amount
     */
    private static void sendDamageFeedbackPacket(ServerPlayer player, long damage) {
        try {
            DamageFeedbackSyncPacket packet = new DamageFeedbackSyncPacket(
                convertCultivationToVanillaDamage(damage),
                "unknown_damage"
            );
            NetworkRegistry.sendToPlayer(packet, player);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error sending damage feedback packet: {}", UTILS_NAME, e.getMessage(), e);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Gets the damage type multiplier for immunity calculation.
     * This allows different damage types to have different immunity characteristics.
     *
     * @param damageSource The damage source (can be null)
     * @return The multiplier for this damage type
     */
    private static double getDamageTypeMultiplier(DamageSource damageSource) {
        if (damageSource == null) {
            return 1.0;
        }

        try {
            String damageType = damageSource.type().msgId();

            // Cache damage type information for performance
            DamageType typeInfo = damageTypeCache.computeIfAbsent(damageType, key -> {
                return analyzeDamageType(damageSource);
            });

            return typeInfo.immunityMultiplier;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error getting damage type multiplier: {}", UTILS_NAME, e.getMessage(), e);
            return 1.0; // Default multiplier
        }
    }

    /**
     * Analyzes a damage source to determine its characteristics.
     *
     * @param damageSource The damage source to analyze
     * @return DamageType information
     */
    private static DamageType analyzeDamageType(DamageSource damageSource) {
        try {
            String typeId = damageSource.type().msgId();

            // Define multipliers based on damage type
            double multiplier = switch (typeId.toLowerCase()) {
                case "fall", "drown", "starve" -> 0.5; // Shorter immunity for environmental damage
                case "explosion", "lightning_bolt" -> 2.0; // Longer immunity for high-impact damage
                case "magic", "indirect_magic" -> 1.5; // Moderate immunity for magical damage
                default -> 1.0; // Standard immunity for most damage types
            };

            return new DamageType(typeId, multiplier);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error analyzing damage type: {}", UTILS_NAME, e.getMessage(), e);
            return new DamageType("unknown", 1.0);
        }
    }

    // === INNER CLASSES === //

    /**
     * Data class for damage type information
     */
    private static class DamageType {
        public final String id;
        public final double immunityMultiplier;

        public DamageType(String id, double immunityMultiplier) {
            this.id = id;
            this.immunityMultiplier = immunityMultiplier;
        }
    }
}