package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles RESPAWN-SPECIFIC logic for the cultivation system.
 * This handler focuses ONLY on what should happen when a player dies and respawns,
 * such as cultivation progress penalties and resource restoration.
 *
 * IMPORTANT: This works in conjunction with DimensionChangeHandler, which handles
 * the technical aspects of player reference updates and data synchronization.
 * This handler runs AFTER DimensionChangeHandler to apply respawn-specific changes.
 *
 * Compatible with the permanent additive stat system - only modifies temporary
 * values like cultivation progress and current resources, never permanent bonuses.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerRespawnHandler {

    /**
     * Handle respawn-specific cultivation logic.
     * Runs at LOWEST priority to ensure DimensionChangeHandler has already
     * updated player references and synced base data.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Applying respawn penalties and restoration for player {}",
            player.getName().getString());

        try {
            // Apply cultivation progress penalties (death should have consequences)
            applyCultivationDeathPenalties(player);

            // Restore resources to full (respawn should heal you)
            restoreResourcesOnRespawn(player);

            // Send respawn message to player
            sendRespawnMessage(player);

            ImmortalCultivation.LOGGER.info("Respawn processing complete for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing respawn for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Apply cultivation progress penalties on death.
     * In Xianxia tradition, death should have consequences but not completely destroy progress.
     * This preserves permanent bonuses (pills, realm achievements) but reduces current progress.
     */
    private static void applyCultivationDeathPenalties(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation attachment during respawn!",
                    player.getName().getString());
                return;
            }

            // Calculate death penalties (lose 10% of current cultivation progress)
            long currentQiCultivation = cultivation.getCurrentQiCultivation();
            long currentBodyCultivation = cultivation.getCurrentBodyCultivation();

            long maxQiCultivation = cultivation.getMaxQiCultivation();
            long maxBodyCultivation = cultivation.getMaxBodyCultivation();

            // Apply penalty: lose 10% of current progress, minimum 0
            long qiPenalty = Math.max(0, currentQiCultivation / 10);
            long bodyPenalty = Math.max(0, currentBodyCultivation / 10);

            long newQiCultivation = Math.max(0, currentQiCultivation - qiPenalty);
            long newBodyCultivation = Math.max(0, currentBodyCultivation - bodyPenalty);

            // Apply the penalties
            cultivation.setCurrentQiCultivation(newQiCultivation);
            cultivation.setCurrentBodyCultivation(newBodyCultivation);

            // Reset regeneration fractions to prevent accumulation issues
            cultivation.setRegenFraction(0.0);

            // IMPORTANT: Don't touch permanent bonuses (pills, realm achievements)
            // Those should persist through death as they represent permanent character growth

            // Log the penalties applied
            if (qiPenalty > 0 || bodyPenalty > 0) {
                ImmortalCultivation.LOGGER.info("Applied death penalties to player {} - Qi: -{} ({}/{}), Body: -{} ({}/{}))",
                    player.getName().getString(),
                    qiPenalty, newQiCultivation, maxQiCultivation,
                    bodyPenalty, newBodyCultivation, maxBodyCultivation);
            } else {
                ImmortalCultivation.LOGGER.info("No death penalties applied to player {} (no progress to lose)",
                    player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying cultivation death penalties for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Restore all resources to full on respawn.
     * Uses the attachment's own methods to ensure compatibility with the permanent additive system.
     */
    private static void restoreResourcesOnRespawn(ServerPlayer player) {
        try {
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources == null) {
                ImmortalCultivation.LOGGER.error("Player {} has null resource attachment during respawn!",
                    player.getName().getString());
                return;
            }

            // Force recalculation of max values based on current permanent bonuses
            // This ensures compatibility with the permanent additive system
            resources.forceHealthRecalculation();

            // Restore to full resources
            resources.setCurrentHealth(resources.getMaxHealth());
            resources.setCurrentQi(resources.getMaxQi());
            resources.setCurrentBody(resources.getMaxBody());

            // Reset regeneration fractions
            resources.setRegenFraction(0.0);

            // Update vanilla health bar to match our custom health system
            // Calculate percentage of vanilla health to set
            float vanillaHealthPercentage = Math.min(1.0f, (float) resources.getCurrentHealth() / (float) resources.getMaxHealth());
            player.setHealth(player.getMaxHealth() * vanillaHealthPercentage);

            ImmortalCultivation.LOGGER.info("Restored resources for player {} - Health: {}/{}, Qi: {}/{}, Body: {}/{}",
                player.getName().getString(),
                resources.getCurrentHealth(), resources.getMaxHealth(),
                resources.getCurrentQi(), resources.getMaxQi(),
                resources.getCurrentBody(), resources.getMaxBody());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error restoring resources for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Send a respawn message to the player explaining what happened.
     * This provides feedback about the death penalties and restoration.
     */
    private static void sendRespawnMessage(ServerPlayer player) {
        try {
            // Get current cultivation state for message
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) return;

            Component respawnMessage = Component.literal("⚰️ You have returned from the void... ⚰️\n")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("Your soul bears the scars of death, but your journey continues.\n")
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("• Some cultivation progress has been lost to the underworld\n")
                    .withStyle(ChatFormatting.RED))
                .append(Component.literal("• Your body and spirit have been restored to full strength\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("• Permanent achievements remain with your soul\n")
                    .withStyle(ChatFormatting.GOLD));

            player.sendSystemMessage(respawnMessage);

            // Show current cultivation state
            Component cultivationState = Component.literal("Current State: ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("Qi %s (%d/%d), Body %s (%d/%d)",
                        cultivation.getCurrentQiRealm().getDisplayName(),
                        cultivation.getCurrentQiCultivation(),
                        cultivation.getMaxQiCultivation(),
                        cultivation.getCurrentBodyRealm().getDisplayName(),
                        cultivation.getCurrentBodyCultivation(),
                        cultivation.getMaxBodyCultivation()))
                    .withStyle(ChatFormatting.WHITE));

            player.sendSystemMessage(cultivationState);

        } catch (Exception e) {
            // Don't spam errors for cosmetic messages
            ImmortalCultivation.LOGGER.debug("Error sending respawn message for player {}",
                player.getName().getString());
        }
    }
}