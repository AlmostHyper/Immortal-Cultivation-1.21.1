package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.BonusSource;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeModifierUtils {

    /**
     * Example method for pills to add stat bonuses (if you want pills to affect stats directly).
     * This would go in your pill consumption logic as an alternative or addition to cultivation rate bonuses.
     *
     * @param player The player consuming the pill
     * @param statType The stat type to boost
     * @param bonusAmount The amount of bonus to add
     */
    public static void addPillStatBonus(ServerPlayer player, IStatsData.StatType statType, long bonusAmount) {
        PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (stats != null && bonusAmount > 0) {
            long currentPillBonus = stats.getSourcedStatBonus(statType, BonusSource.PILL);
            stats.setSourcedStatBonus(statType, BonusSource.PILL, currentPillBonus + bonusAmount);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Added pill stat bonus for {}: +{} {} (total from pills: {})",
                    player.getName().getString(), bonusAmount, statType,
                    stats.getSourcedStatBonus(statType, BonusSource.PILL));
            }
        }
    }

    /**
     * Example method for pills to add regeneration bonuses.
     *
     * @param player The player consuming the pill
     * @param statType The stat type to boost regeneration for
     * @param bonusAmount The regeneration bonus to add
     */
    public static void addPillRegenBonus(ServerPlayer player, IStatsData.StatType statType, double bonusAmount) {
        PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (stats != null && bonusAmount > 0.0) {
            double currentPillBonus = stats.getSourcedStatRegenBonus(statType, BonusSource.PILL);
            stats.setSourcedStatRegenBonus(statType, BonusSource.PILL, currentPillBonus + bonusAmount);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Added pill regen bonus for {}: +{} {} regen (total from pills: {})",
                    player.getName().getString(), bonusAmount, statType,
                    stats.getSourcedStatRegenBonus(statType, BonusSource.PILL));
            }
        }
    }

    /**
     * Checks if a player is immune to a specific damage source
     *
     * @param playerId Player UUID
     * @param sourceKey Damage source key
     * @return True if player is immune
     */
    private static boolean isPlayerImmune(UUID playerId, String sourceKey) {
        Map<String, Long> playerImmunities = playerImmunityMap.get(playerId);
        if (playerImmunities == null) {
            return false;
        }

        Long immunityEnd = playerImmunities.get(sourceKey);
        if (immunityEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() > immunityEnd) {
            playerImmunities.remove(sourceKey);
            return false;
        }

        return true;
    }

    /**
     * Sets immunity for a player against a specific damage source
     *
     * @param playerId Player UUID
     * @param sourceKey Damage source key
     * @param durationMs Immunity duration in milliseconds
     */
    private static void setPlayerImmunity(UUID playerId, String sourceKey, long durationMs) {
        playerImmunityMap.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(sourceKey, System.currentTimeMillis() + durationMs);
    }

    /**
     * Clears all immunity for a player
     *
     * @param playerId Player UUID
     */
    private static void clearPlayerImmunity(UUID playerId) {
        playerImmunityMap.remove(playerId);
    }

    /**
     * Recalculates all player attributes based on current equipment and cultivation
     *
     * @param player The player
     */
    private static void recalculatePlayerAttributes(ServerPlayer player) {
        // Clear existing modifiers and recalculate
        // This should integrate with your cultivation attribute system

        AttributeInstance attackAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            // Remove old modifiers and add new ones based on cultivation
            // Implementation depends on your attribute system
        }
    }

    /**
     * Refreshes and recalculates all player stats after permanent bonuses are applied
     * from cultivation realm advancement, pills, or other permanent modifiers.
     * This handles the complete PlayerStatsAttachment system including Health, Qi, Body,
     * attack damage, movement speed, and jump height.
     *
     * @param player The server player whose stats need to be refreshed
     */
    public static void refreshStats(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Cannot refresh stats for null player");
            return;
        }

        try {
            PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
            if (statsAttachment == null) {
                ImmortalCultivation.LOGGER.warn("Cannot refresh stats: PlayerStatsAttachment not found for player {}",
                    player.getName().getString());
                return;
            }

            // Force recalculation of all stat maximums based on permanent bonuses
            // This includes Health, Qi, and Body maximum calculations
            statsAttachment.recalculateAll();

            // Update vanilla health display to match custom health system
            updateVanillaHealthDisplay(player, statsAttachment);

            // Apply all attribute modifiers (combat stats)
            applyAllAttributeModifiers(player, statsAttachment);

            // Mark attachment for client synchronization
            statsAttachment.markDirty();

            ImmortalCultivation.LOGGER.debug("Successfully refreshed all stats for player: {} - H:{}/{} Q:{}/{} B:{}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                statsAttachment.getMaxStat(IStatsData.StatType.QI),
                statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                statsAttachment.getMaxStat(IStatsData.StatType.BODY));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error refreshing stats for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Applies all attribute modifiers (attack damage, movement speed, jump height)
     * based on permanent cultivation bonuses. Uses unique ResourceLocations to avoid conflicts with
     * equipment-based modifiers handled by recalculatePlayerAttributes.
     *
     * @param player The server player to apply modifiers to
     * @param statsAttachment The stats attachment containing permanent bonuses
     */
    private static void applyAllAttributeModifiers(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            // Use specific ResourceLocations for cultivation-based modifiers (different from equipment modifiers)
            ResourceLocation cultivationSpeedModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_speed_bonus");
            ResourceLocation cultivationAttackModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_attack_bonus");
            ResourceLocation cultivationJumpModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_jump_bonus");

            // Apply movement speed modifier
            AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double totalSpeedBonus = statsAttachment.getPermanentMovementSpeedBonus() +
                    statsAttachment.getMovementSpeedBonus();

                // Remove existing cultivation speed modifier
                movementSpeed.removeModifier(cultivationSpeedModifierId);

                if (totalSpeedBonus != 0.0) {
                    movementSpeed.addPermanentModifier(new AttributeModifier(
                        cultivationSpeedModifierId,
                        totalSpeedBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // Apply attack damage modifier
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double totalAttackBonus = statsAttachment.getPermanentAttackDamageBonus() +
                    statsAttachment.getAttackDamageBonus();

                // Remove existing cultivation attack modifier
                attackDamage.removeModifier(cultivationAttackModifierId);

                if (totalAttackBonus != 0.0) {
                    attackDamage.addPermanentModifier(new AttributeModifier(
                        cultivationAttackModifierId,
                        totalAttackBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // Apply jump height modifier
            AttributeInstance jumpStrength = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpStrength != null) {
                double totalJumpBonus = statsAttachment.getPermanentJumpHeightBonus() +
                    statsAttachment.getJumpHeightBonus();

                // Remove existing cultivation jump modifier
                jumpStrength.removeModifier(cultivationJumpModifierId);

                if (totalJumpBonus != 0.0) {
                    jumpStrength.addPermanentModifier(new AttributeModifier(
                        cultivationJumpModifierId,
                        totalJumpBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // Apply step height modifier (add this after the jump height section)
            AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeight != null) {
                double totalJumpBonus = statsAttachment.getPermanentJumpHeightBonus() +
                    statsAttachment.getJumpHeightBonus();

                // Scale step height with jump bonus (typically smaller multiplier than jump)
                double stepHeightBonus = totalJumpBonus * 0.5; // 50% of jump bonus for step height

                // Remove existing cultivation step height modifier
                ResourceLocation cultivationStepModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_step_bonus");
                stepHeight.removeModifier(cultivationStepModifierId);

                if (stepHeightBonus != 0.0) {
                    stepHeight.addPermanentModifier(new AttributeModifier(
                        cultivationStepModifierId,
                        stepHeightBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("Applied step height modifier: {} for player {}",
                            stepHeightBonus, player.getName().getString());
                    }
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying attribute modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }
}
