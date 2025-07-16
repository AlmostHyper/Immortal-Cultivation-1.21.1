package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.BonusSource;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Utility class for managing Minecraft attribute modifiers based on cultivation progression.
 *
 * This util class contains shared logic for applying and managing attribute modifiers
 * that affect player movement speed, attack damage, jump height, and other Minecraft attributes
 * based on cultivation bonuses from realms, pills, equipment, and other sources.
 *
 * Key Responsibilities:
 * - Apply/remove cultivation-based attribute modifiers
 * - Handle equipment attribute integration
 * - Manage pill-based attribute bonuses
 * - Provide utilities for attribute recalculation
 * - Support debugging and monitoring of attribute changes
 *
 * Architecture:
 * - Util Pattern: Contains shared business logic for attribute management
 * - Integrates with EventManager through handler classes
 * - Uses PlayerEventUtils for complex stat operations
 * - Follows DRY principles for attribute modifier operations
 */
public class AttributeModifierUtils {

    // === CONSTANTS === //
    // No longer need individual constants - these are now handled by the AttributeType enum

    // === TRACKING MAPS === //
    private static final Map<String, ResourceLocation> MODIFIER_IDS = new ConcurrentHashMap<>();
    private static final Map<String, String> MODIFIER_DESCRIPTIONS = new ConcurrentHashMap<>();

    // === INITIALIZATION === //
    static {
        initializeModifierMappings();
    }

    /**
     * Initializes the modifier ID mappings for consistent reference
     */
    private static void initializeModifierMappings() {
        // Initialize modifier IDs based on AttributeType enum
        for (AttributeType attributeType : AttributeType.values()) {
            String key = attributeType.getKey();
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivation.MOD_ID,
                "cultivation_" + key + "_bonus"
            );
            MODIFIER_IDS.put(key, modifierId);
            MODIFIER_DESCRIPTIONS.put(key, "Cultivation " + attributeType.getDisplayName() + " Bonus");
        }

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Initialized modifier mappings for {} types",
                AttributeType.values().length);
        }
    }

    // === PUBLIC API METHODS === //

    /**
     * Refreshes all player stats and attributes based on current cultivation state.
     * This is the main entry point for updating player attributes and should be called
     * when cultivation state changes, realm advancement occurs, or on player login/respawn.
     *
     * @param player The server player whose stats and attributes need refreshing
     */
    public static void refreshPlayerAttributes(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("[AttributeModifierUtils] Cannot refresh attributes for null player");
            return;
        }

        try {
            PlayerStatsAttachment statsAttachment = player.getData(AttachmentRegistry.PLAYER_STATS.get());
            if (statsAttachment == null) {
                ImmortalCultivation.LOGGER.warn("[AttributeModifierUtils] Cannot refresh attributes: PlayerStatsAttachment not found for player {}",
                    player.getName().getString());
                return;
            }

            // Force recalculation of all stat maximums based on permanent bonuses
            statsAttachment.recalculateAll();

            // Apply all cultivation-based attribute modifiers
            applyAllCultivationModifiers(player, statsAttachment);

            // Update vanilla health display (handled by PlayerEventUtils)
            PlayerEventUtils.updateVanillaHealthDisplay(player, statsAttachment);

            // Mark attachment for client synchronization
            statsAttachment.markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Successfully refreshed attributes for player: {} - H:{}/{} Q:{}/{} B:{}/{}",
                    player.getName().getString(),
                    statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                    statsAttachment.getMaxStat(IStatsData.StatType.QI),
                    statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                    statsAttachment.getMaxStat(IStatsData.StatType.BODY));
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error refreshing attributes for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Adds a stat bonus from pill consumption to a player's stats.
     * This method integrates with the pill consumption system and applies
     * permanent bonuses that persist through death.
     *
     * @param player The player consuming the pill
     * @param statType The stat type to boost (HEALTH, QI, BODY)
     * @param bonusAmount The amount of bonus to add
     */
    public static void addPillStatBonus(ServerPlayer player, IStatsData.StatType statType, long bonusAmount) {
        if (player == null || bonusAmount <= 0) {
            return;
        }

        try {
            PlayerStatsAttachment stats = player.getData(AttachmentRegistry.PLAYER_STATS.get());
            if (stats != null) {
                long currentPillBonus = stats.getSourcedStatBonus(statType, BonusSource.PILL);
                stats.setSourcedStatBonus(statType, BonusSource.PILL, currentPillBonus + bonusAmount);

                // Refresh attributes to apply the new bonus
                refreshPlayerAttributes(player);

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Added pill stat bonus for {}: +{} {} (total from pills: {})",
                        player.getName().getString(), bonusAmount, statType,
                        stats.getSourcedStatBonus(statType, BonusSource.PILL));
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error adding pill stat bonus for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles equipment change events to recalculate attribute modifiers.
     * This method ensures cultivation bonuses work properly alongside equipment bonuses.
     *
     * @param player The player whose equipment changed
     */
    public static void handleEquipmentChange(ServerPlayer player) {
        if (player == null) {
            return;
        }

        try {
            // Small delay to ensure equipment changes are processed
            // This prevents conflicts between equipment and cultivation modifiers
            refreshPlayerAttributes(player);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Handled equipment change for player: {}",
                    player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error handling equipment change for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    // === PRIVATE UTILITY METHODS === //

    /**
     * Applies all cultivation-based attribute modifiers to a player.
     * This method handles movement speed, attack damage, jump height, and step height
     * based on permanent cultivation bonuses from realms, pills, and other sources.
     *
     * @param player The server player to apply modifiers to
     * @param statsAttachment The stats attachment containing cultivation bonuses
     */
    private static void applyAllCultivationModifiers(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            // Apply standard attribute modifiers using generic method
            applyAttributeModifier(player, AttributeType.SPEED, statsAttachment);
            applyAttributeModifier(player, AttributeType.ATTACK, statsAttachment);
            applyAttributeModifier(player, AttributeType.JUMP, statsAttachment);
            applyAttributeModifier(player, AttributeType.STEP, statsAttachment);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Applied all cultivation modifiers for player: {}",
                    player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error applying cultivation modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Generic method to apply an attribute modifier based on attribute type.
     * This follows the same pattern as the attachment system for handling different types generically.
     *
     * @param player The server player to apply the modifier to
     * @param attributeType The type of attribute to modify
     * @param statsAttachment The stats attachment containing cultivation bonuses
     */
    private static void applyAttributeModifier(ServerPlayer player, AttributeType attributeType, PlayerStatsAttachment statsAttachment) {
        try {
            // Get the appropriate attribute instance
            AttributeInstance attributeInstance = attributeType.getAttribute(player);
            if (attributeInstance == null) {
                return;
            }

            // Get the modifier ID and calculate bonus amount
            ResourceLocation modifierId = attributeType.getModifierId();
            double bonusAmount = attributeType.calculateBonus(statsAttachment);

            // Remove existing cultivation modifier
            attributeInstance.removeModifier(modifierId);

            // Apply new modifier if bonus is non-zero
            if (bonusAmount != 0.0) {
                attributeInstance.addPermanentModifier(new AttributeModifier(
                    modifierId,
                    bonusAmount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Applied {} modifier: {} for player {}",
                        attributeType.getDisplayName(), bonusAmount, player.getName().getString());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error applying {} modifier for player {}: {}",
                attributeType.getDisplayName(), player.getName().getString(), e.getMessage(), e);
        }
    }

    // === ATTRIBUTE TYPE ENUM === //

    /**
     * Enum defining different attribute types and their behavior.
     * This follows the same pattern as cultivation types in the attachment system.
     */
    private enum AttributeType {
        SPEED("speed", "Movement Speed") {
            @Override
            public AttributeInstance getAttribute(Player player) {
                return player.getAttribute(Attributes.MOVEMENT_SPEED);
            }

            @Override
            public double calculateBonus(PlayerStatsAttachment stats) {
                return stats.getPermanentMovementSpeedBonus() + stats.getMovementSpeedBonus();
            }
        },

        ATTACK("attack", "Attack Damage") {
            @Override
            public AttributeInstance getAttribute(Player player) {
                return player.getAttribute(Attributes.ATTACK_DAMAGE);
            }

            @Override
            public double calculateBonus(PlayerStatsAttachment stats) {
                return stats.getPermanentAttackDamageBonus() + stats.getAttackDamageBonus();
            }
        },

        JUMP("jump", "Jump Height") {
            @Override
            public AttributeInstance getAttribute(Player player) {
                return player.getAttribute(Attributes.JUMP_STRENGTH);
            }

            @Override
            public double calculateBonus(PlayerStatsAttachment stats) {
                return stats.getPermanentJumpHeightBonus() + stats.getJumpHeightBonus();
            }
        },

        STEP("step", "Step Height") {
            @Override
            public AttributeInstance getAttribute(Player player) {
                return player.getAttribute(Attributes.STEP_HEIGHT);
            }

            @Override
            public double calculateBonus(PlayerStatsAttachment stats) {
                // Step height scales with jump bonus (50% of jump bonus)
                double jumpBonus = stats.getPermanentJumpHeightBonus() + stats.getJumpHeightBonus();
                return jumpBonus * 0.5;
            }
        };

        private final String key;
        private final String displayName;

        AttributeType(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        /**
         * Gets the Minecraft attribute instance for this attribute type
         *
         * @param player The player to get the attribute from
         * @return The attribute instance or null if not available
         */
        public abstract AttributeInstance getAttribute(Player player);

        /**
         * Calculates the total bonus amount for this attribute type
         *
         * @param stats The player's stats attachment
         * @return The total bonus amount to apply
         */
        public abstract double calculateBonus(PlayerStatsAttachment stats);

        /**
         * Gets the ResourceLocation modifier ID for this attribute type
         *
         * @return The modifier ID
         */
        public ResourceLocation getModifierId() {
            return MODIFIER_IDS.get(key);
        }

        /**
         * Gets the display name for this attribute type
         *
         * @return The display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Gets the key for this attribute type
         *
         * @return The key
         */
        public String getKey() {
            return key;
        }
    }

    // === DEBUGGING AND INTROSPECTION === //

    /**
     * Gets debug information about all applied cultivation modifiers for a player
     *
     * @param player The player to get modifier information for
     * @return A map of modifier types to their current values
     */
    public static Map<String, Double> getAppliedModifiers(Player player) {
        Map<String, Double> appliedModifiers = new ConcurrentHashMap<>();

        try {
            for (AttributeType attributeType : AttributeType.values()) {
                AttributeInstance attribute = attributeType.getAttribute(player);
                if (attribute != null) {
                    AttributeModifier modifier = attribute.getModifier(attributeType.getModifierId());
                    if (modifier != null) {
                        appliedModifiers.put(attributeType.getKey(), modifier.amount());
                    }
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error getting applied modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }

        return appliedModifiers;
    }

    /**
     * Removes all cultivation-based modifiers from a player (used for cleanup)
     *
     * @param player The player to remove modifiers from
     */
    public static void removeAllCultivationModifiers(Player player) {
        try {
            for (AttributeType attributeType : AttributeType.values()) {
                AttributeInstance attribute = attributeType.getAttribute(player);
                if (attribute != null) {
                    attribute.removeModifier(attributeType.getModifierId());
                }
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[AttributeModifierUtils] Removed all cultivation modifiers for player: {}",
                    player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[AttributeModifierUtils] Error removing cultivation modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Gets detailed debug information about all attribute types and their states
     *
     * @param player The player to get debug information for
     * @return A formatted string containing debug information
     */
    public static String getDebugInformation(Player player) {
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== Cultivation Attribute Debug Info ===\n");
        debugInfo.append("Player: ").append(player.getName().getString()).append("\n");

        try {
            PlayerStatsAttachment stats = player.getData(AttachmentRegistry.PLAYER_STATS.get());
            if (stats == null) {
                debugInfo.append("ERROR: No PlayerStatsAttachment found!\n");
                return debugInfo.toString();
            }

            for (AttributeType attributeType : AttributeType.values()) {
                debugInfo.append("\n--- ").append(attributeType.getDisplayName()).append(" ---\n");

                AttributeInstance attribute = attributeType.getAttribute(player);
                if (attribute != null) {
                    double calculatedBonus = attributeType.calculateBonus(stats);
                    AttributeModifier appliedModifier = attribute.getModifier(attributeType.getModifierId());

                    debugInfo.append("  Base Value: ").append(attribute.getBaseValue()).append("\n");
                    debugInfo.append("  Current Value: ").append(attribute.getValue()).append("\n");
                    debugInfo.append("  Calculated Bonus: ").append(calculatedBonus).append("\n");
                    debugInfo.append("  Applied Modifier: ").append(appliedModifier != null ? appliedModifier.amount() : "None").append("\n");
                } else {
                    debugInfo.append("  ERROR: Attribute not found!\n");
                }
            }

        } catch (Exception e) {
            debugInfo.append("ERROR: ").append(e.getMessage()).append("\n");
        }

        return debugInfo.toString();
    }
}