package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles all cultivation-related ticking for players.
 * This includes both Qi and Body cultivation paths, as well as meditation detection,
 * resource regeneration, and bonus management for the dual cultivation system.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class CultivationTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Process all cultivation attachments - they handle their own logic now
        tickCultivationAttachments(serverPlayer);

        // Process meditation detection for both cultivation types
        processMeditationDetection(serverPlayer);

        // Process resource regeneration (Qi, Body energy, AND Health - FIXED!)
        processResourceRegeneration(serverPlayer);

        // Tick combat stats attachment
        tickCombatStats(serverPlayer);
    }

    /**
     * CRITICAL FIX: Tick cultivation attachment and ensure player reference is set
     */
    private static void tickCultivationAttachments(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                // CRITICAL: Ensure player reference is set before ticking
                cultivation.setPlayer(player);
                cultivation.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking cultivation attachments for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * FIXED: Process resource regeneration including Health, Qi Energy, and Body Energy.
     * These are the consumable resources (like mana/stamina), not cultivation progress.
     */
    private static void processResourceRegeneration(ServerPlayer player) {
        try {
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                resources.setPlayer(player); // Ensure player reference is set
                resources.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing resource regeneration for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Tick combat stats attachment for sync management and internal logic.
     * This ensures that combat bonuses are properly maintained and synced to the client.
     */
    private static void tickCombatStats(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStatsAttachment != null) {
                combatStatsAttachment.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking combat stats attachment for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Determine if a player is in meditation posture.
     * Meditation requires the player to be crouching and relatively stationary.
     * This is used by both Qi and Body cultivation for enhanced cultivation rates.
     */
    private static boolean isPlayerMeditating(ServerPlayer player) {
        // Check if player is crouching (shift key held)
        if (!player.isCrouching()) {
            return false;
        }

        // Check velocity to see if player is moving
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Allow very small movement (account for server lag/float precision)
        return horizontalSpeed < 0.01;
    }

    /**
     * Process meditation detection for both Qi and Body cultivation.
     * Meditation enhances cultivation rates significantly, so we need to detect
     * when players are in the proper meditation posture.
     */
    private static void processMeditationDetection(ServerPlayer player) {
        try {
            boolean isCurrentlyMeditating = isPlayerMeditating(player);

            // Update resource attachment with meditation state
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                if (isCurrentlyMeditating) {
                    resources.startMeditation();
                } else {
                    resources.stopMeditation();
                }
            }

            // Update cultivation attachment with meditation state for potential future use
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                if (isCurrentlyMeditating) {
                    cultivation.startMeditation();
                } else {
                    cultivation.stopMeditation();
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing meditation detection for player {}",
                player.getName().getString(), e);
        }
    }
}