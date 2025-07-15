package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.content.item.spirit_stones.SpiritStoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Tooltip Event Utilities for the Immortal Cultivation Mod
 *
 * Contains business logic for tooltip enhancement, following DRY principles
 * and maintaining separation of concerns. This utility class handles the core
 * tooltip enhancement mechanics while leaving event subscription to handler classes.
 *
 * Key Features:
 * - Spirit stone tooltip enhancement with cultivation bonuses
 * - Equipment tooltip enhancement with aspect and material bonuses
 * - Player-specific effectiveness calculations and requirements
 * - Cultivation realm compatibility information
 * - Generic item enhancement detection and display
 *
 * Architecture:
 * - Static utility methods for stateless operations
 * - Integration with player attachments for context-sensitive tooltips
 * - Modular tooltip enhancement by item type
 * - Comprehensive error handling and graceful fallbacks
 */
public class TooltipEventUtils {

    // === CONSTANTS === //

    private static final String UTILS_NAME = "TooltipEventUtils";

    // Tooltip styling constants
    private static final ChatFormatting HEADER_COLOR = ChatFormatting.GOLD;
    private static final ChatFormatting VALUE_COLOR = ChatFormatting.YELLOW;
    private static final ChatFormatting BONUS_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting PENALTY_COLOR = ChatFormatting.RED;
    private static final ChatFormatting INFO_COLOR = ChatFormatting.GRAY;
    private static final ChatFormatting REQUIREMENT_COLOR = ChatFormatting.DARK_RED;

    // Item type identifiers
    private static final String ASPECTED_SUFFIX = "_aspected";
    private static final String SKY_IRON_PREFIX = "sky_iron";
    private static final String SPIRIT_STONE_SUFFIX = "_spirit_stone";
    private static final String CULTIVATION_PREFIX = "cultivation_";

    // === CORE TOOLTIP PROCESSING === //

    /**
     * Enhances an item's tooltip with cultivation-specific information.
     * Determines the appropriate enhancement type based on item characteristics
     * and applies relevant tooltip modifications.
     *
     * @param stack The item stack to enhance
     * @param tooltip The tooltip component list to modify
     * @param player The player viewing the tooltip (can be null)
     * @param statsAttachment The player's stats attachment (can be null)
     * @param cultivationAttachment The player's cultivation attachment (can be null)
     * @return True if the tooltip was modified, false otherwise
     */
    public static boolean enhanceItemTooltip(ItemStack stack,
                                             List<Component> tooltip,
                                             @Nullable Player player,
                                             @Nullable PlayerStatsAttachment statsAttachment,
                                             @Nullable PlayerCultivationAttachment cultivationAttachment) {

        if (stack.isEmpty() || tooltip == null) {
            return false;
        }

        boolean tooltipModified = false;
        String itemRegistryName = getItemRegistryName(stack);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Enhancing tooltip for item: {}",
                UTILS_NAME, itemRegistryName);
        }

        try {
            // Enhance spirit stone tooltips
            if (isSpiritStone(stack, itemRegistryName)) {
                tooltipModified |= enhanceSpiritStoneTooltip(stack, tooltip, player, statsAttachment);
            }

            // Enhance cultivation equipment tooltips
            if (isCultivationEquipment(stack, itemRegistryName)) {
                tooltipModified |= enhanceEquipmentTooltip(stack, tooltip, player, statsAttachment);
            }

            // Enhance aspected equipment tooltips
            if (isAspectedEquipment(itemRegistryName)) {
                tooltipModified |= enhanceAspectedEquipmentTooltip(stack, tooltip, player, cultivationAttachment);
            }

            // Add general cultivation information for any cultivation-related item
            if (isCultivationRelated(itemRegistryName)) {
                tooltipModified |= addGeneralCultivationInfo(stack, tooltip, player, statsAttachment, cultivationAttachment);
            }

            // Add player-specific contextual information
            if (player != null && (statsAttachment != null || cultivationAttachment != null)) {
                tooltipModified |= addPlayerContextualInfo(stack, tooltip, player, statsAttachment, cultivationAttachment);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error enhancing tooltip for item {}: {}",
                UTILS_NAME, itemRegistryName, e.getMessage(), e);
        }

        if (ImmortalCultivation.LOGGER.isDebugEnabled() && tooltipModified) {
            ImmortalCultivation.LOGGER.debug("[{}] Successfully enhanced tooltip for: {}",
                UTILS_NAME, itemRegistryName);
        }

        return tooltipModified;
    }

    // === SPECIFIC TOOLTIP ENHANCEMENTS === //

    /**
     * Enhances spirit stone tooltips with cultivation bonuses and effectiveness.
     *
     * @param stack The spirit stone item stack
     * @param tooltip The tooltip list to modify
     * @param player The player viewing the tooltip
     * @param statsAttachment The player's stats attachment
     * @return True if tooltip was modified
     */
    private static boolean enhanceSpiritStoneTooltip(ItemStack stack,
                                                     List<Component> tooltip,
                                                     @Nullable Player player,
                                                     @Nullable PlayerStatsAttachment statsAttachment) {

        String itemName = getItemRegistryName(stack);
        SpiritStoneQuality quality = determineSpiritStoneQuality(itemName);

        if (quality == null) {
            return false;
        }

        // Add quality information
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Spirit Stone Quality: ")
            .withStyle(INFO_COLOR)
            .append(Component.literal(quality.getDisplayName())
                .withStyle(quality.getColor())));

        // Add cultivation bonuses
        long qiBonus = quality.getQiBonus();
        long bodyBonus = quality.getBodyBonus();

        if (qiBonus > 0) {
            tooltip.add(Component.literal("◆ Qi Cultivation: +")
                .withStyle(INFO_COLOR)
                .append(Component.literal(String.valueOf(qiBonus))
                    .withStyle(BONUS_COLOR))
                .append(Component.literal(" per use")
                    .withStyle(INFO_COLOR)));
        }

        if (bodyBonus > 0) {
            tooltip.add(Component.literal("◆ Body Cultivation: +")
                .withStyle(INFO_COLOR)
                .append(Component.literal(String.valueOf(bodyBonus))
                    .withStyle(BONUS_COLOR))
                .append(Component.literal(" per use")
                    .withStyle(INFO_COLOR)));
        }

        // Add player-specific effectiveness if available
        if (player != null && statsAttachment != null) {
            addSpiritStoneEffectiveness(tooltip, quality, statsAttachment);
        }

        return true;
    }

    /**
     * Enhances equipment tooltips with cultivation bonuses.
     *
     * @param stack The equipment item stack
     * @param tooltip The tooltip list to modify
     * @param player The player viewing the tooltip
     * @param statsAttachment The player's stats attachment
     * @return True if tooltip was modified
     */
    private static boolean enhanceEquipmentTooltip(ItemStack stack,
                                                   List<Component> tooltip,
                                                   @Nullable Player player,
                                                   @Nullable PlayerStatsAttachment statsAttachment) {

        String itemName = getItemRegistryName(stack);
        EquipmentBonuses bonuses = calculateEquipmentBonuses(stack, itemName);

        if (!bonuses.hasAnyBonus()) {
            return false;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Cultivation Bonuses:")
            .withStyle(HEADER_COLOR));

        if (bonuses.healthBonus != 0) {
            addBonusLine(tooltip, "Health", bonuses.healthBonus);
        }

        if (bonuses.qiBonus != 0) {
            addBonusLine(tooltip, "Qi", bonuses.qiBonus);
        }

        if (bonuses.bodyBonus != 0) {
            addBonusLine(tooltip, "Body", bonuses.bodyBonus);
        }

        if (bonuses.attackDamageBonus != 0.0) {
            tooltip.add(Component.literal("◆ Attack Damage: +")
                .withStyle(INFO_COLOR)
                .append(Component.literal(String.format("%.1f%%", bonuses.attackDamageBonus * 100))
                    .withStyle(BONUS_COLOR)));
        }

        if (bonuses.movementSpeedBonus != 0.0) {
            tooltip.add(Component.literal("◆ Movement Speed: +")
                .withStyle(INFO_COLOR)
                .append(Component.literal(String.format("%.1f%%", bonuses.movementSpeedBonus * 100))
                    .withStyle(BONUS_COLOR)));
        }

        return true;
    }

    /**
     * Enhances aspected equipment tooltips with aspect information.
     *
     * @param stack The aspected equipment item stack
     * @param tooltip The tooltip list to modify
     * @param player The player viewing the tooltip
     * @param cultivationAttachment The player's cultivation attachment
     * @return True if tooltip was modified
     */
    private static boolean enhanceAspectedEquipmentTooltip(ItemStack stack,
                                                           List<Component> tooltip,
                                                           @Nullable Player player,
                                                           @Nullable PlayerCultivationAttachment cultivationAttachment) {

        String itemName = getItemRegistryName(stack);
        String aspect = extractAspectFromName(itemName);

        if (aspect.equals("unknown")) {
            return false;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Aspect: ")
            .withStyle(INFO_COLOR)
            .append(Component.literal(capitalizeFirstLetter(aspect))
                .withStyle(getAspectColor(aspect))));

        // Add aspect-specific bonuses description
        tooltip.add(Component.literal("◆ Enhanced effectiveness with ")
            .withStyle(INFO_COLOR)
            .append(Component.literal(aspect + " cultivation")
                .withStyle(getAspectColor(aspect))));

        // Add player compatibility if available
        if (player != null && cultivationAttachment != null) {
            addAspectCompatibility(tooltip, aspect, cultivationAttachment);
        }

        return true;
    }

    /**
     * Adds general cultivation information to any cultivation-related item.
     *
     * @param stack The item stack
     * @param tooltip The tooltip list to modify
     * @param player The player viewing the tooltip
     * @param statsAttachment The player's stats attachment
     * @param cultivationAttachment The player's cultivation attachment
     * @return True if tooltip was modified
     */
    private static boolean addGeneralCultivationInfo(ItemStack stack,
                                                     List<Component> tooltip,
                                                     @Nullable Player player,
                                                     @Nullable PlayerStatsAttachment statsAttachment,
                                                     @Nullable PlayerCultivationAttachment cultivationAttachment) {

        // Only add if this item doesn't already have specific cultivation info
        // (to avoid duplicate information)
        if (hasExistingCultivationTooltip(tooltip)) {
            return false;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Cultivation Item")
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        return true;
    }

    /**
     * Adds player-specific contextual information to tooltips.
     *
     * @param stack The item stack
     * @param tooltip The tooltip list to modify
     * @param player The player viewing the tooltip
     * @param statsAttachment The player's stats attachment
     * @param cultivationAttachment The player's cultivation attachment
     * @return True if tooltip was modified
     */
    private static boolean addPlayerContextualInfo(ItemStack stack,
                                                   List<Component> tooltip,
                                                   @Nullable Player player,
                                                   @Nullable PlayerStatsAttachment statsAttachment,
                                                   @Nullable PlayerCultivationAttachment cultivationAttachment) {

        if (player == null) {
            return false;
        }

        boolean added = false;

        // Add realm requirement checks for advanced items
        if (isAdvancedCultivationItem(stack)) {
            added |= addRealmRequirementInfo(tooltip, stack, cultivationAttachment);
        }

        return added;
    }

    // === HELPER METHODS === //

    /**
     * Adds a bonus line to the tooltip with appropriate formatting.
     *
     * @param tooltip The tooltip list to modify
     * @param statName The name of the stat
     * @param bonus The bonus value
     */
    private static void addBonusLine(List<Component> tooltip, String statName, long bonus) {
        ChatFormatting valueColor = bonus > 0 ? BONUS_COLOR : PENALTY_COLOR;
        String prefix = bonus > 0 ? "+" : "";

        tooltip.add(Component.literal("◆ " + statName + ": ")
            .withStyle(INFO_COLOR)
            .append(Component.literal(prefix + bonus)
                .withStyle(valueColor)));
    }

    /**
     * Adds spirit stone effectiveness information based on player stats.
     *
     * @param tooltip The tooltip list to modify
     * @param quality The spirit stone quality
     * @param statsAttachment The player's stats attachment
     */
    private static void addSpiritStoneEffectiveness(List<Component> tooltip,
                                                    SpiritStoneQuality quality,
                                                    PlayerStatsAttachment statsAttachment) {

        // Calculate effectiveness based on current cultivation level
        // Higher cultivation levels might have reduced effectiveness from lower quality stones
        double effectiveness = calculateSpiritStoneEffectiveness(quality, statsAttachment);

        if (effectiveness < 1.0) {
            tooltip.add(Component.literal("◆ Current Effectiveness: ")
                .withStyle(INFO_COLOR)
                .append(Component.literal(String.format("%.1f%%", effectiveness * 100))
                    .withStyle(effectiveness >= 0.8 ? BONUS_COLOR :
                        effectiveness >= 0.5 ? ChatFormatting.YELLOW : PENALTY_COLOR)));
        }
    }

    /**
     * Adds aspect compatibility information for the player.
     *
     * @param tooltip The tooltip list to modify
     * @param aspect The equipment aspect
     * @param cultivationAttachment The player's cultivation attachment
     */
    private static void addAspectCompatibility(List<Component> tooltip,
                                               String aspect,
                                               PlayerCultivationAttachment cultivationAttachment) {

        // Check if player has compatible cultivation or talents
        // This is a placeholder - you would implement based on your cultivation system
        tooltip.add(Component.literal("◆ Compatibility: ")
            .withStyle(INFO_COLOR)
            .append(Component.literal("Compatible")
                .withStyle(BONUS_COLOR)));
    }

    /**
     * Adds realm requirement information for advanced items.
     *
     * @param tooltip The tooltip list to modify
     * @param stack The item stack
     * @param cultivationAttachment The player's cultivation attachment
     * @return True if information was added
     */
    private static boolean addRealmRequirementInfo(List<Component> tooltip,
                                                   ItemStack stack,
                                                   @Nullable PlayerCultivationAttachment cultivationAttachment) {

        // Determine required realm based on item rarity or name
        String requiredRealm = determineRequiredRealm(stack);

        if (requiredRealm == null) {
            return false;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Required Realm: ")
            .withStyle(INFO_COLOR)
            .append(Component.literal(requiredRealm)
                .withStyle(REQUIREMENT_COLOR)));

        return true;
    }

    // === UTILITY METHODS === //

    /**
     * Gets the registry name of an item stack.
     *
     * @param stack The item stack
     * @return The registry name string
     */
    private static String getItemRegistryName(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation location = stack.getItem().builtInRegistryHolder().key().location();
        return location.toString();
    }

    /**
     * Checks if an item is a spirit stone.
     *
     * @param stack The item stack
     * @param itemName The item registry name
     * @return True if the item is a spirit stone
     */
    private static boolean isSpiritStone(ItemStack stack, String itemName) {
        return stack.getItem() instanceof SpiritStoneItem || itemName.contains(SPIRIT_STONE_SUFFIX);
    }

    /**
     * Checks if an item is cultivation equipment.
     *
     * @param stack The item stack
     * @param itemName The item registry name
     * @return True if the item is cultivation equipment
     */
    private static boolean isCultivationEquipment(ItemStack stack, String itemName) {
        return (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof SwordItem ||
            stack.getItem() instanceof TieredItem) &&
            (itemName.contains(SKY_IRON_PREFIX) || itemName.contains(CULTIVATION_PREFIX));
    }

    /**
     * Checks if an item is aspected equipment.
     *
     * @param itemName The item registry name
     * @return True if the item is aspected equipment
     */
    private static boolean isAspectedEquipment(String itemName) {
        return itemName.contains(ASPECTED_SUFFIX);
    }

    /**
     * Checks if an item is cultivation-related.
     *
     * @param itemName The item registry name
     * @return True if the item is cultivation-related
     */
    private static boolean isCultivationRelated(String itemName) {
        return itemName.contains(ImmortalCultivation.MOD_ID);
    }

    /**
     * Checks if an item is an advanced cultivation item requiring realm checks.
     *
     * @param stack The item stack
     * @return True if the item is advanced
     */
    private static boolean isAdvancedCultivationItem(ItemStack stack) {
        return stack.getRarity() == Rarity.EPIC || stack.getRarity() == Rarity.RARE;
    }

    /**
     * Checks if tooltip already contains cultivation information.
     *
     * @param tooltip The tooltip list to check
     * @return True if cultivation info already exists
     */
    private static boolean hasExistingCultivationTooltip(List<Component> tooltip) {
        return tooltip.stream()
            .anyMatch(component -> component.getString().toLowerCase().contains("cultivation"));
    }

    /**
     * Determines spirit stone quality from item name.
     *
     * @param itemName The item registry name
     * @return The spirit stone quality or null if not determinable
     */
    @Nullable
    private static SpiritStoneQuality determineSpiritStoneQuality(String itemName) {
        if (itemName.contains("low_quality")) return SpiritStoneQuality.LOW;
        if (itemName.contains("medium_quality")) return SpiritStoneQuality.MEDIUM;
        if (itemName.contains("high_quality")) return SpiritStoneQuality.HIGH;
        if (itemName.contains("top_quality")) return SpiritStoneQuality.TOP;
        return null;
    }

    /**
     * Calculates equipment bonuses for an item.
     *
     * @param stack The item stack
     * @param itemName The item registry name
     * @return Equipment bonuses data
     */
    private static EquipmentBonuses calculateEquipmentBonuses(ItemStack stack, String itemName) {
        EquipmentBonuses bonuses = new EquipmentBonuses();

        // Sky Iron equipment bonuses
        if (itemName.contains(SKY_IRON_PREFIX)) {
            bonuses.healthBonus = 50L;
            bonuses.qiBonus = 25L;
            bonuses.attackDamageBonus = 0.1; // 10% increase
        }

        // General cultivation equipment bonuses based on rarity
        if (stack.getRarity() == Rarity.RARE) {
            bonuses.healthBonus += 25L;
            bonuses.qiBonus += 15L;
        } else if (stack.getRarity() == Rarity.EPIC) {
            bonuses.healthBonus += 50L;
            bonuses.qiBonus += 30L;
            bonuses.bodyBonus += 20L;
        }

        return bonuses;
    }

    /**
     * Calculates spirit stone effectiveness based on player cultivation level.
     *
     * @param quality The spirit stone quality
     * @param statsAttachment The player's stats attachment
     * @return Effectiveness multiplier (0.0 to 1.0)
     */
    private static double calculateSpiritStoneEffectiveness(SpiritStoneQuality quality,
                                                            PlayerStatsAttachment statsAttachment) {

        // Get player's current cultivation levels
        long qiCultivation = statsAttachment.getCurrentStat(IStatsData.StatType.QI);
        long bodyCultivation = statsAttachment.getCurrentStat(IStatsData.StatType.BODY);
        long averageCultivation = (qiCultivation + bodyCultivation) / 2;

        // Calculate effectiveness based on quality vs cultivation level
        double effectiveness = 1.0;
        long qualityThreshold = quality.getEffectivenessThreshold();

        if (averageCultivation > qualityThreshold) {
            // Diminishing returns for overleveled stones
            double ratio = (double) averageCultivation / qualityThreshold;
            effectiveness = Math.max(0.1, 1.0 / Math.sqrt(ratio));
        }

        return effectiveness;
    }

    /**
     * Extracts aspect type from equipment name.
     *
     * @param itemName The item registry name
     * @return The aspect type string
     */
    private static String extractAspectFromName(String itemName) {
        String[] aspects = {"earth", "fire", "metal", "water", "wood"};
        for (String aspect : aspects) {
            if (itemName.contains(aspect)) {
                return aspect;
            }
        }
        return "unknown";
    }

    /**
     * Gets the display color for an aspect.
     *
     * @param aspect The aspect name
     * @return The appropriate ChatFormatting color
     */
    private static ChatFormatting getAspectColor(String aspect) {
        return switch (aspect.toLowerCase()) {
            case "earth" -> ChatFormatting.YELLOW;
            case "fire" -> ChatFormatting.RED;
            case "metal" -> ChatFormatting.WHITE;
            case "water" -> ChatFormatting.BLUE;
            case "wood" -> ChatFormatting.GREEN;
            default -> ChatFormatting.GRAY;
        };
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str The string to capitalize
     * @return The capitalized string
     */
    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Determines required realm for an item.
     *
     * @param stack The item stack
     * @return The required realm name or null
     */
    @Nullable
    private static String determineRequiredRealm(ItemStack stack) {
        // Map rarity to realm requirements
        return switch (stack.getRarity()) {
            case RARE -> "Foundation Establishment";
            case EPIC -> "Core Formation";
            default -> null;
        };
    }

    // === DATA CLASSES === //

    /**
     * Enumeration for spirit stone qualities and their properties.
     */
    private enum SpiritStoneQuality {
        LOW("Low Quality", ChatFormatting.GRAY, 25L, 15L, 1000L),
        MEDIUM("Medium Quality", ChatFormatting.GREEN, 50L, 30L, 2500L),
        HIGH("High Quality", ChatFormatting.BLUE, 100L, 60L, 5000L),
        TOP("Top Quality", ChatFormatting.LIGHT_PURPLE, 200L, 120L, 10000L);

        private final String displayName;
        private final ChatFormatting color;
        private final long qiBonus;
        private final long bodyBonus;
        private final long effectivenessThreshold;

        SpiritStoneQuality(String displayName, ChatFormatting color, long qiBonus, long bodyBonus, long effectivenessThreshold) {
            this.displayName = displayName;
            this.color = color;
            this.qiBonus = qiBonus;
            this.bodyBonus = bodyBonus;
            this.effectivenessThreshold = effectivenessThreshold;
        }

        public String getDisplayName() { return displayName; }
        public ChatFormatting getColor() { return color; }
        public long getQiBonus() { return qiBonus; }
        public long getBodyBonus() { return bodyBonus; }
        public long getEffectivenessThreshold() { return effectivenessThreshold; }
    }

    /**
     * Data class for equipment bonuses.
     */
    private static class EquipmentBonuses {
        long healthBonus = 0L;
        long qiBonus = 0L;
        long bodyBonus = 0L;
        double attackDamageBonus = 0.0;
        double movementSpeedBonus = 0.0;

        /**
         * Checks if this equipment provides any bonuses.
         *
         * @return True if any bonus is non-zero
         */
        boolean hasAnyBonus() {
            return healthBonus != 0 || qiBonus != 0 || bodyBonus != 0 ||
                attackDamageBonus != 0.0 || movementSpeedBonus != 0.0;
        }
    }
}