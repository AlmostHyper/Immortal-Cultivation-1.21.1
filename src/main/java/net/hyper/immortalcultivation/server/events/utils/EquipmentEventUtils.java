package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.BonusSource;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.server.events.server.EquipmentEventHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;

import java.util.Map;
import java.util.UUID;

/**
 * Equipment Event Utilities for the Immortal Cultivation Mod
 *
 * Contains business logic for equipment-related event processing, following DRY principles
 * and maintaining separation of concerns. This utility class handles the core equipment
 * mechanics while leaving event subscription to handler classes.
 *
 * Key Features:
 * - Equipment bonus calculation and application using BonusSource.EQUIPMENT
 * - Aspected equipment recognition and stat modification
 * - Vanilla attribute integration with cultivation system
 * - Equipment change detection and efficient processing
 * - Performance optimized calculations and caching
 *
 * Architecture:
 * - Static utility methods for stateless operations
 * - Integration with AttributeModifierUtils for vanilla compatibility
 * - Thread-safe operations for server environments
 * - Comprehensive error handling and logging
 */
public class EquipmentEventUtils {

    // === CONSTANTS === //

    private static final String UTILS_NAME = "EquipmentEventUtils";

    // Equipment modifier IDs for vanilla attributes (separate from cultivation modifiers)
    private static final ResourceLocation EQUIPMENT_ATTACK_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "equipment_attack_bonus");
    private static final ResourceLocation EQUIPMENT_SPEED_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "equipment_speed_bonus");
    private static final ResourceLocation EQUIPMENT_ARMOR_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "equipment_armor_bonus");

    // Equipment type identifiers for aspected gear
    private static final String ASPECTED_SUFFIX = "_aspected";
    private static final String SKY_IRON_PREFIX = "sky_iron";

    // === CORE EQUIPMENT PROCESSING === //

    /**
     * Processes an equipment change for a player through the cultivation system.
     * Handles equipment bonuses, stat modifications, and attribute updates.
     *
     * @param player The server player changing equipment
     * @param statsAttachment The player's stats attachment
     * @param slot The equipment slot being changed
     * @param oldStack The previous item stack
     * @param newStack The new item stack
     * @param snapshotMap The equipment snapshot tracking map
     */
    public static void processEquipmentChange(ServerPlayer player,
                                              PlayerStatsAttachment statsAttachment,
                                              EquipmentSlot slot,
                                              ItemStack oldStack,
                                              ItemStack newStack,
                                              Map<UUID, EquipmentEventHandler.EquipmentSnapshot> snapshotMap) {

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing equipment change for player: {} in slot: {}",
                UTILS_NAME, player.getName().getString(), slot.getName());
        }

        try {
            // Remove bonuses from old equipment
            if (!oldStack.isEmpty()) {
                removeEquipmentBonuses(player, statsAttachment, slot, oldStack);
            }

            // Apply bonuses from new equipment
            if (!newStack.isEmpty()) {
                applyEquipmentBonuses(player, statsAttachment, slot, newStack);
            }

            // Recalculate vanilla attributes to maintain compatibility
            recalculateVanillaAttributes(player, statsAttachment);

            // Update equipment snapshot
            snapshotMap.put(player.getUUID(), new EquipmentEventHandler.EquipmentSnapshot(player));

            // Mark attachment for sync
            statsAttachment.markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Successfully processed equipment change for player: {}",
                    UTILS_NAME, player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing equipment change for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Forces a complete equipment recalculation for a player.
     * Clears all equipment bonuses and reapplies them based on current equipment.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     * @param snapshotMap The equipment snapshot tracking map
     */
    public static void forceFullEquipmentRecalculation(ServerPlayer player,
                                                       PlayerStatsAttachment statsAttachment,
                                                       Map<UUID, EquipmentEventHandler.EquipmentSnapshot> snapshotMap) {

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Forcing full equipment recalculation for player: {}",
                UTILS_NAME, player.getName().getString());
        }

        try {
            // Clear all equipment-based bonuses
            clearAllEquipmentBonuses(statsAttachment);

            // Reapply bonuses for all current equipment
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    applyEquipmentBonuses(player, statsAttachment, slot, stack);
                }
            }

            // Recalculate vanilla attributes
            recalculateVanillaAttributes(player, statsAttachment);

            // Update equipment snapshot
            snapshotMap.put(player.getUUID(), new EquipmentEventHandler.EquipmentSnapshot(player));

            // Mark attachment for sync
            statsAttachment.markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Completed full equipment recalculation for player: {}",
                    UTILS_NAME, player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during full equipment recalculation for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === EQUIPMENT BONUS MANAGEMENT === //

    /**
     * Applies equipment bonuses to a player's stats based on equipped item.
     * Recognizes aspected equipment and applies appropriate cultivation bonuses.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     * @param slot The equipment slot
     * @param stack The item stack to analyze
     */
    private static void applyEquipmentBonuses(ServerPlayer player,
                                              PlayerStatsAttachment statsAttachment,
                                              EquipmentSlot slot,
                                              ItemStack stack) {

        String itemName = getItemRegistryName(stack);
        EquipmentBonuses bonuses = calculateEquipmentBonuses(stack, slot, itemName);

        if (bonuses.hasAnyBonus()) {
            // Apply stat bonuses using the equipment source
            if (bonuses.healthBonus != 0) {
                long currentBonus = statsAttachment.getSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.EQUIPMENT,
                    currentBonus + bonuses.healthBonus);
            }

            if (bonuses.qiBonus != 0) {
                long currentBonus = statsAttachment.getSourcedStatBonus(IStatsData.StatType.QI, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, BonusSource.EQUIPMENT,
                    currentBonus + bonuses.qiBonus);
            }

            if (bonuses.bodyBonus != 0) {
                long currentBonus = statsAttachment.getSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.EQUIPMENT,
                    currentBonus + bonuses.bodyBonus);
            }

            // Apply combat bonuses using the equipment source
            if (bonuses.attackDamageBonus != 0.0) {
                double currentBonus = statsAttachment.getSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.EQUIPMENT,
                    currentBonus + bonuses.attackDamageBonus);
            }

            if (bonuses.movementSpeedBonus != 0.0) {
                double currentBonus = statsAttachment.getSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.EQUIPMENT,
                    currentBonus + bonuses.movementSpeedBonus);
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Applied equipment bonuses for {}: {} (slot: {})",
                    UTILS_NAME, player.getName().getString(), bonuses, slot.getName());
            }
        }
    }

    /**
     * Removes equipment bonuses from a player's stats for an unequipped item.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     * @param slot The equipment slot
     * @param stack The item stack being removed
     */
    private static void removeEquipmentBonuses(ServerPlayer player,
                                               PlayerStatsAttachment statsAttachment,
                                               EquipmentSlot slot,
                                               ItemStack stack) {

        String itemName = getItemRegistryName(stack);
        EquipmentBonuses bonuses = calculateEquipmentBonuses(stack, slot, itemName);

        if (bonuses.hasAnyBonus()) {
            // Remove stat bonuses
            if (bonuses.healthBonus != 0) {
                long currentBonus = statsAttachment.getSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.EQUIPMENT,
                    currentBonus - bonuses.healthBonus);
            }

            if (bonuses.qiBonus != 0) {
                long currentBonus = statsAttachment.getSourcedStatBonus(IStatsData.StatType.QI, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, BonusSource.EQUIPMENT,
                    currentBonus - bonuses.qiBonus);
            }

            if (bonuses.bodyBonus != 0) {
                long currentBonus = statsAttachment.getSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.EQUIPMENT,
                    currentBonus - bonuses.bodyBonus);
            }

            // Remove combat bonuses
            if (bonuses.attackDamageBonus != 0.0) {
                double currentBonus = statsAttachment.getSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.EQUIPMENT,
                    currentBonus - bonuses.attackDamageBonus);
            }

            if (bonuses.movementSpeedBonus != 0.0) {
                double currentBonus = statsAttachment.getSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.EQUIPMENT);
                statsAttachment.setSourcedCombatBonus(
                    PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.EQUIPMENT,
                    currentBonus - bonuses.movementSpeedBonus);
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Removed equipment bonuses for {}: {} (slot: {})",
                    UTILS_NAME, player.getName().getString(), bonuses, slot.getName());
            }
        }
    }

    /**
     * Clears all equipment-based bonuses from a player's stats.
     * Used during full recalculation or cleanup operations.
     *
     * @param statsAttachment The player's stats attachment
     */
    private static void clearAllEquipmentBonuses(PlayerStatsAttachment statsAttachment) {
        // Clear all stat bonuses from equipment
        statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.EQUIPMENT, 0L);
        statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, BonusSource.EQUIPMENT, 0L);
        statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.EQUIPMENT, 0L);

        // Clear all combat bonuses from equipment
        statsAttachment.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.EQUIPMENT, 0.0);
        statsAttachment.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.EQUIPMENT, 0.0);
        statsAttachment.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.JUMP_HEIGHT, BonusSource.EQUIPMENT, 0.0);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared all equipment bonuses", UTILS_NAME);
        }
    }

    // === EQUIPMENT BONUS CALCULATION === //

    /**
     * Calculates the bonuses provided by a specific piece of equipment.
     * Recognizes aspected gear and applies appropriate cultivation bonuses.
     *
     * @param stack The item stack to analyze
     * @param slot The equipment slot
     * @param itemName The registry name of the item
     * @return Equipment bonuses object
     */
    private static EquipmentBonuses calculateEquipmentBonuses(ItemStack stack, EquipmentSlot slot, String itemName) {
        EquipmentBonuses bonuses = new EquipmentBonuses();

        // Base bonuses from item type
        if (stack.getItem() instanceof ArmorItem armorItem) {
            bonuses.healthBonus = calculateArmorHealthBonus(armorItem, slot);
            bonuses.bodyBonus = calculateArmorBodyBonus(armorItem, slot);
        }

        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem) {
            bonuses.attackDamageBonus = calculateWeaponAttackBonus(stack);
        }

        // Aspected equipment bonuses
        if (isAspectedEquipment(itemName)) {
            String aspect = extractAspectFromName(itemName);
            applyAspectedBonuses(bonuses, aspect, slot);
        }

        // Sky Iron equipment bonuses (higher tier)
        if (isSkyIronEquipment(itemName)) {
            applySkyIronBonuses(bonuses, slot);
        }

        return bonuses;
    }

    /**
     * Calculates health bonus for armor pieces.
     *
     * @param armorItem The armor item
     * @param slot The equipment slot
     * @return Health bonus amount
     */
    private static long calculateArmorHealthBonus(ArmorItem armorItem, EquipmentSlot slot) {
        // Base health bonus scaling with armor protection
        int armorValue = armorItem.getDefense();
        return switch (slot) {
            case HEAD -> armorValue * 5L;
            case CHEST -> armorValue * 8L;
            case LEGS -> armorValue * 6L;
            case FEET -> armorValue * 4L;
            default -> 0L;
        };
    }

    /**
     * Calculates body cultivation bonus for armor pieces.
     *
     * @param armorItem The armor item
     * @param slot The equipment slot
     * @return Body bonus amount
     */
    private static long calculateArmorBodyBonus(ArmorItem armorItem, EquipmentSlot slot) {
        // Body bonus is typically lower than health bonus
        long healthBonus = calculateArmorHealthBonus(armorItem, slot);
        return healthBonus / 2; // 50% of health bonus
    }

    /**
     * Calculates attack damage bonus for weapons.
     *
     * @param stack The weapon item stack
     * @return Attack damage bonus multiplier
     */
    private static double calculateWeaponAttackBonus(ItemStack stack) {
        if (stack.getItem() instanceof TieredItem tieredItem) {
            float attackDamage = tieredItem.getTier().getAttackDamageBonus();
            return attackDamage * 0.1; // 10% per attack damage point
        }
        return 0.0;
    }

    // === ASPECTED EQUIPMENT HANDLING === //

    /**
     * Applies bonuses specific to aspected equipment.
     *
     * @param bonuses The bonuses object to modify
     * @param aspect The aspect type (earth, fire, metal, water, wood)
     * @param slot The equipment slot
     */
    private static void applyAspectedBonuses(EquipmentBonuses bonuses, String aspect, EquipmentSlot slot) {
        // Base aspected bonus
        long aspectBonus = switch (slot) {
            case HEAD, FEET -> 10L;
            case CHEST -> 20L;
            case LEGS -> 15L;
            case MAINHAND -> 12L;
            default -> 5L;
        };

        // Apply aspect-specific bonuses
        switch (aspect.toLowerCase()) {
            case "earth" -> {
                bonuses.healthBonus += aspectBonus;
                bonuses.bodyBonus += aspectBonus / 2;
            }
            case "fire" -> {
                bonuses.attackDamageBonus += 0.15; // 15% attack bonus
                bonuses.qiBonus += aspectBonus / 2;
            }
            case "metal" -> {
                bonuses.attackDamageBonus += 0.10; // 10% attack bonus
                bonuses.bodyBonus += aspectBonus;
            }
            case "water" -> {
                bonuses.qiBonus += aspectBonus;
                bonuses.movementSpeedBonus += 0.05; // 5% speed bonus
            }
            case "wood" -> {
                bonuses.healthBonus += aspectBonus / 2;
                bonuses.qiBonus += aspectBonus / 2;
                bonuses.bodyBonus += aspectBonus / 2;
            }
        }
    }

    /**
     * Applies bonuses specific to Sky Iron equipment (higher tier).
     *
     * @param bonuses The bonuses object to modify
     * @param slot The equipment slot
     */
    private static void applySkyIronBonuses(EquipmentBonuses bonuses, EquipmentSlot slot) {
        // Sky Iron provides balanced bonuses with higher values
        long skyIronBonus = switch (slot) {
            case HEAD, FEET -> 25L;
            case CHEST -> 40L;
            case LEGS -> 30L;
            case MAINHAND -> 35L;
            default -> 15L;
        };

        bonuses.healthBonus += skyIronBonus;
        bonuses.qiBonus += skyIronBonus;
        bonuses.bodyBonus += skyIronBonus;
        bonuses.attackDamageBonus += 0.20; // 20% attack bonus
        bonuses.movementSpeedBonus += 0.10; // 10% speed bonus
    }

    // === VANILLA ATTRIBUTE INTEGRATION === //

    /**
     * Recalculates vanilla attributes based on current equipment bonuses.
     * Maintains compatibility with vanilla systems while respecting cultivation stats.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     */
    private static void recalculateVanillaAttributes(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            // Delegate to existing AttributeModifierUtils for cultivation-based attributes
            AttributeModifierUtils.refreshPlayerAttributes(player);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Recalculated vanilla attributes for player: {}",
                    UTILS_NAME, player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error recalculating vanilla attributes for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
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
     * Checks if an item is aspected equipment.
     *
     * @param itemName The item registry name
     * @return True if the item is aspected equipment
     */
    private static boolean isAspectedEquipment(String itemName) {
        return itemName.contains(ASPECTED_SUFFIX);
    }

    /**
     * Checks if an item is Sky Iron equipment.
     *
     * @param itemName The item registry name
     * @return True if the item is Sky Iron equipment
     */
    private static boolean isSkyIronEquipment(String itemName) {
        return itemName.contains(SKY_IRON_PREFIX);
    }

    /**
     * Extracts the aspect type from an aspected equipment name.
     *
     * @param itemName The item registry name
     * @return The aspect type string (earth, fire, metal, water, wood)
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

    // === EQUIPMENT BONUSES DATA CLASS === //

    /**
     * Data class representing bonuses provided by a piece of equipment.
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
            return healthBonus != 0L || qiBonus != 0L || bodyBonus != 0L ||
                attackDamageBonus != 0.0 || movementSpeedBonus != 0.0;
        }

        @Override
        public String toString() {
            return String.format("EquipmentBonuses{health=%d, qi=%d, body=%d, attack=%.2f, speed=%.2f}",
                healthBonus, qiBonus, bodyBonus, attackDamageBonus, movementSpeedBonus);
        }
    }
}