package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Comprehensive handler for maintaining cultivation data integrity across dimension changes.
 * Ensures all attachment player references are updated and data is properly synchronized.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class DimensionChangeHandler {

    /**
     * CRITICAL: This event fires AFTER the player has been moved to the new dimension
     * but BEFORE the player starts ticking in the new dimension. This is the perfect
     * time to update all attachment player references and force synchronization.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Player {} changed dimension from {} to {} - updating attachment references",
            player.getName().getString(),
            event.getFrom().location(),
            event.getTo().location());

        try {
            // Update player references in ALL attachments
            updateAllAttachmentPlayerReferences(player);

            // Force sync all data to ensure client has correct values
            forceSyncAllAttachmentData(player);

            // Reapply all attribute modifiers (health, combat stats, etc.)
            reapplyAllAttributeModifiers(player);

            ImmortalCultivation.LOGGER.info("Successfully updated all cultivation data for player {} after dimension change",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating cultivation data for player {} during dimension change",
                player.getName().getString(), e);
        }
    }

    /**
     * Updates player references in all cultivation-related attachments.
     * This is critical because dimension changes create new player entity instances.
     */
    private static void updateAllAttachmentPlayerReferences(ServerPlayer player) {
        try {
            // Update PlayerCultivationAttachment
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                cultivation.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Updated player reference for PlayerCultivationAttachment");
            }

            // Update PlayerResourceAttachment
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                resources.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Updated player reference for PlayerResourceAttachment");
            }

            // Update PlayerCombatStatsAttachment
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                combatStats.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Updated player reference for PlayerCombatStatsAttachment");
            }

            // Update PlayerTalentAttachment
            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talents != null) {
                talents.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Updated player reference for PlayerTalentAttachment");
            }

            // Update PlayerRaceAttachment
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race != null) {
                race.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Updated player reference for PlayerRaceAttachment");
            }

            // Update PlayerPillConsumptionAttachment
            PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
            if (pillConsumption != null) {
                pillConsumption.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Updated player reference for PlayerPillConsumptionAttachment");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating attachment player references for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Forces synchronization of all attachment data to ensure the client
     * has the correct values after dimension change.
     */
    private static void forceSyncAllAttachmentData(ServerPlayer player) {
        try {
            // Sync cultivation data
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                cultivation.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced PlayerCultivationAttachment");
            }

            // Sync resource data
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                resources.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced PlayerResourceAttachment");
            }

            // Sync combat stats
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                combatStats.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced PlayerCombatStatsAttachment");
            }

            // Sync talent data
            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talents != null) {
                talents.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced PlayerTalentAttachment");
            }

            // Sync race data
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race != null) {
                race.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced PlayerRaceAttachment");
            }

            // Sync pill consumption data
            PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
            if (pillConsumption != null) {
                pillConsumption.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced PlayerPillConsumptionAttachment");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing attachment data for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Reapplies all attribute modifiers (health, combat stats, etc.) to ensure
     * they work correctly in the new dimension.
     */
    private static void reapplyAllAttributeModifiers(ServerPlayer player) {
        try {
            // Reapply combat stats modifiers (this calls your existing method)
            CombatStatsEventHandler.refreshCombatStats(player);
            ImmortalCultivation.LOGGER.debug("Reapplied combat stats modifiers");

            // Trigger health recalculation by updating max health
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                // Force recalculation of max health from cultivation
                resources.forceHealthRecalculation();
                ImmortalCultivation.LOGGER.debug("Recalculated max health from cultivation");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error reapplying attribute modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Additional safety net: Handle player respawn events which can also
     * cause similar issues with attachment player references.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Player {} respawned - updating attachment references",
            player.getName().getString());

        try {
            // Same process as dimension change
            updateAllAttachmentPlayerReferences(player);
            forceSyncAllAttachmentData(player);
            reapplyAllAttributeModifiers(player);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating cultivation data for player {} during respawn",
                player.getName().getString(), e);
        }
    }

    /**
     * Handle player login to ensure everything is properly initialized.
     * This complements your existing PlayerLoginHandler.
     */
    @SubscribeEvent(priority = EventPriority.LOW) // Run after other login handlers
    public static void onPlayerLoginLate(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Delay execution slightly to ensure all other systems have initialized
        player.server.execute(() -> {
            try {
                ImmortalCultivation.LOGGER.debug("Late login processing for player {}", player.getName().getString());

                // Ensure all player references are set (redundant but safe)
                updateAllAttachmentPlayerReferences(player);

                // Force sync everything to make sure client is up to date
                forceSyncAllAttachmentData(player);

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error in late login processing for player {}",
                    player.getName().getString(), e);
            }
        });
    }
}