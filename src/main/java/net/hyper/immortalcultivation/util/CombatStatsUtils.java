package net.hyper.immortalcultivation.util;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCombatStatsAttachment;
import net.hyper.immortalcultivation.handler.CombatStatsEventHandler;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for managing combat stats bonuses.
 * Provides convenient methods for adding bonuses from items, abilities, pills, etc.
 */
public class CombatStatsUtils {

    // === BONUS APPLICATION METHODS ===

    /**
     * Add attack damage bonus to a player
     * @param player The player to add bonus to
     * @param bonusPercent Bonus as percentage (e.g., 10.0 for +10%)
     * @param source Description of what granted the bonus
     * @return true if bonus was successfully added
     */
    public static boolean addAttackDamageBonus(Player player, double bonusPercent, String source) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                ImmortalCultivation.LOGGER.error("Cannot add attack damage bonus - no combat stats attachment for player {}",
                    player.getName().getString());
                return false;
            }

            double bonusDecimal = bonusPercent / 100.0;
            combatStats.addAttackDamageBonus(bonusDecimal);

            // Notify player
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Gained ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("+" + String.format("%.1f", bonusPercent) + "% Attack Damage")
                            .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" from " + source)
                            .withStyle(ChatFormatting.GRAY))
                );

                // Refresh combat stats immediately
                CombatStatsEventHandler.refreshCombatStats(serverPlayer);
            }

            ImmortalCultivation.LOGGER.info("Added {}% attack damage bonus to player {} from {}",
                String.format("%.1f", bonusPercent), player.getName().getString(), source);
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error adding attack damage bonus to player {}",
                player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Add movement speed bonus to a player
     * @param player The player to add bonus to
     * @param bonusPercent Bonus as percentage (e.g., 15.0 for +15%)
     * @param source Description of what granted the bonus
     * @return true if bonus was successfully added
     */
    public static boolean addMovementSpeedBonus(Player player, double bonusPercent, String source) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                ImmortalCultivation.LOGGER.error("Cannot add movement speed bonus - no combat stats attachment for player {}",
                    player.getName().getString());
                return false;
            }

            double bonusDecimal = bonusPercent / 100.0;
            combatStats.addMovementSpeedBonus(bonusDecimal);

            // Notify player
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Gained ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("+" + String.format("%.1f", bonusPercent) + "% Movement Speed")
                            .withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" from " + source)
                            .withStyle(ChatFormatting.GRAY))
                );

                // Refresh combat stats immediately
                CombatStatsEventHandler.refreshCombatStats(serverPlayer);
            }

            ImmortalCultivation.LOGGER.info("Added {}% movement speed bonus to player {} from {}",
                String.format("%.1f", bonusPercent), player.getName().getString(), source);
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error adding movement speed bonus to player {}",
                player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Add jump height bonus to a player
     * @param player The player to add bonus to
     * @param bonusPercent Bonus as percentage (e.g., 20.0 for +20%)
     * @param source Description of what granted the bonus
     * @return true if bonus was successfully added
     */
    public static boolean addJumpHeightBonus(Player player, double bonusPercent, String source) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                ImmortalCultivation.LOGGER.error("Cannot add jump height bonus - no combat stats attachment for player {}",
                    player.getName().getString());
                return false;
            }

            double bonusDecimal = bonusPercent / 100.0;
            combatStats.addJumpHeightBonus(bonusDecimal);

            // Notify player
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Gained ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("+" + String.format("%.1f", bonusPercent) + "% Jump Height")
                            .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" from " + source)
                            .withStyle(ChatFormatting.GRAY))
                );

                // Refresh combat stats immediately
                CombatStatsEventHandler.refreshCombatStats(serverPlayer);
            }

            ImmortalCultivation.LOGGER.info("Added {}% jump height bonus to player {} from {}",
                String.format("%.1f", bonusPercent), player.getName().getString(), source);
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error adding jump height bonus to player {}",
                player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Add all three combat stat bonuses at once
     * @param player The player to add bonuses to
     * @param attackPercent Attack damage bonus percentage
     * @param speedPercent Movement speed bonus percentage
     * @param jumpPercent Jump height bonus percentage
     * @param source Description of what granted the bonuses
     * @return true if all bonuses were successfully added
     */
    public static boolean addAllCombatBonuses(Player player, double attackPercent, double speedPercent,
                                              double jumpPercent, String source) {
        boolean success = true;

        if (attackPercent > 0) {
            success &= addAttackDamageBonus(player, attackPercent, source);
        }

        if (speedPercent > 0) {
            success &= addMovementSpeedBonus(player, speedPercent, source);
        }

        if (jumpPercent > 0) {
            success &= addJumpHeightBonus(player, jumpPercent, source);
        }

        // Send summary message if all bonuses were added
        if (success && player instanceof ServerPlayer serverPlayer &&
            (attackPercent > 0 || speedPercent > 0 || jumpPercent > 0)) {

            serverPlayer.sendSystemMessage(
                Component.literal("Cultivation enhanced your combat abilities!")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );
        }

        return success;
    }

    // === QUERY METHODS ===

    /**
     * Get current attack damage bonus for a player
     */
    public static double getAttackDamageBonus(Player player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            return combatStats != null ? combatStats.getAttackDamageBonus() * 100.0 : 0.0;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting attack damage bonus for player {}",
                player.getName().getString(), e);
            return 0.0;
        }
    }

    /**
     * Get current movement speed bonus for a player
     */
    public static double getMovementSpeedBonus(Player player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            return combatStats != null ? combatStats.getMovementSpeedBonus() * 100.0 : 0.0;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting movement speed bonus for player {}",
                player.getName().getString(), e);
            return 0.0;
        }
    }

    /**
     * Get current jump height bonus for a player
     */
    public static double getJumpHeightBonus(Player player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            return combatStats != null ? combatStats.getJumpHeightBonus() * 100.0 : 0.0;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting jump height bonus for player {}",
                player.getName().getString(), e);
            return 0.0;
        }
    }

    /**
     * Get total combat power score for a player
     */
    public static double getTotalCombatPower(Player player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            return combatStats != null ? combatStats.getTotalBonusScore() : 0.0;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting total combat power for player {}",
                player.getName().getString(), e);
            return 0.0;
        }
    }

    // === RESET METHODS ===

    /**
     * Reset all combat bonuses for a player (use with caution)
     */
    public static boolean resetAllCombatBonuses(Player player, String reason) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                return false;
            }

            combatStats.resetAllBonuses();

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.literal("All combat bonuses have been reset")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(" (" + reason + ")")
                            .withStyle(ChatFormatting.GRAY))
                );

                // Refresh combat stats immediately
                CombatStatsEventHandler.refreshCombatStats(serverPlayer);
            }

            ImmortalCultivation.LOGGER.info("Reset all combat bonuses for player {} - reason: {}",
                player.getName().getString(), reason);
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting combat bonuses for player {}",
                player.getName().getString(), e);
            return false;
        }
    }

    // === DISPLAY METHODS ===

    /**
     * Get formatted combat stats display for a player
     */
    public static Component getCombatStatsDisplay(Player player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                return Component.literal("No combat stats available").withStyle(ChatFormatting.GRAY);
            }

            return Component.literal("Combat Stats: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("ATK +" + String.format("%.1f", combatStats.getAttackDamageBonus() * 100) + "%")
                    .withStyle(ChatFormatting.RED))
                .append(Component.literal(" | SPD +" + String.format("%.1f", combatStats.getMovementSpeedBonus() * 100) + "%")
                    .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" | JUMP +" + String.format("%.1f", combatStats.getJumpHeightBonus() * 100) + "%")
                    .withStyle(ChatFormatting.YELLOW));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error creating combat stats display for player {}",
                player.getName().getString(), e);
            return Component.literal("Error displaying combat stats").withStyle(ChatFormatting.RED);
        }
    }
}