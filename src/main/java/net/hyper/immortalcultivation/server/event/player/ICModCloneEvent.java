package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles proper attachment copying on player death/clone.
 *
 * This event handler ensures that when a player dies and respawns, all cultivation
 * attachments maintain their data integrity and have proper player references set.
 * It runs at HIGHEST priority to ensure attachments are properly copied before
 * any other respawn processing occurs.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModCloneEvent {

    /**
     * Handles attachment copying and player reference restoration during player clone events.
     *
     * This method ensures that all cultivation attachments are properly copied from the
     * original player to the new player instance, with correct player references set
     * immediately after the copy process. This prevents serialization issues that occur
     * when player references are null during the attachment copy process.
     *
     * @param event The player clone event containing original and new player instances
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) ||
            !(event.getOriginal() instanceof ServerPlayer originalPlayer)) {
            return;
        }

        // Only handle death cloning, not dimension changes
        if (!event.isWasDeath()) {
            return;
        }

        String playerName = newPlayer.getName().getString();
        ImmortalCultivation.LOGGER.info("Processing attachment clone for player death: {}", playerName);

        try {
            // Set player references immediately after attachment copying
            ensurePlayerReferences(newPlayer);

            ImmortalCultivation.LOGGER.debug("Successfully restored player references for cloned attachments: {}", playerName);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Critical error during attachment clone for player {}: {}",
                playerName, e.getMessage(), e);
        }
    }

    /**
     * Ensures all attachments have proper player references set after cloning.
     *
     * This method iterates through all cultivation attachments and sets the player
     * reference to prevent null reference issues during serialization and sync operations.
     *
     * @param player The new player instance that received the cloned attachments
     */
    private static void ensurePlayerReferences(ServerPlayer player) {
        // Set reference for PlayerCultivationAttachment
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation != null) {
            cultivation.setPlayer(player);
        }

        // Set reference for PlayerResourceAttachment
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (resources != null) {
            resources.setPlayer(player);
        }

        // Set reference for PlayerCombatStatsAttachment
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats != null) {
            combatStats.setPlayer(player);
        }

        // Set reference for PlayerBreakthroughAttachment
        PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        if (breakthrough != null) {
            breakthrough.setPlayer(player);
        }

        // Set reference for PlayerTalentAttachment
        PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        if (talents != null) {
            talents.setPlayer(player);
        }

        // Set reference for PlayerRaceAttachment
        PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        if (race != null) {
            race.setPlayer(player);
        }

        // Set reference for PlayerPillConsumptionAttachment
        PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
        if (pillConsumption != null) {
            pillConsumption.setPlayer(player);
        }
    }
}