package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * A generic utility class holding reusable logic for dimension change events.
 * These methods are designed to be called from a central event handler to
 * ensure a player's cultivation data remains consistent across dimensions.
 */
public class ICModDimensionChangeEvent {

    /**
     * Updates all known cultivation-related attachments with the new player instance.
     * This is critical because a new player entity is created when changing dimensions.
     *
     * @param player The new ServerPlayer entity instance.
     */
    public static void updateAllPlayerReferences(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to update references for a null player.");
            return;
        }

        try {
            // Update all attachments with the new player reference.
            player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()).setPlayer(player);
            player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get()).setPlayer(player);
            player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get()).setPlayer(player);
            player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get()).setPlayer(player);
            player.getData(ICModAttachmentTypes.PLAYER_RACE.get()).setPlayer(player);
            player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get()).setPlayer(player);
            player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get()).setPlayer(player);

            ImmortalCultivation.LOGGER.debug("Updated all attachment player references for {}", player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to update one or more player references for {}", player.getName().getString(), e);
        }
    }

    /**
     * Re-applies any attribute modifiers that might be lost during a dimension change.
     * This ensures that stats like bonus health, attack damage, and speed are correct.
     *
     * @param player The player whose attributes need refreshing.
     */
    public static void reapplyAttributeModifiers(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to reapply attributes for a null player.");
            return;
        }

        // Re-calculating health might involve calling a method on the resource attachment
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (resources != null) {
            resources.forceHealthRecalculation();
            ImmortalCultivation.LOGGER.debug("Forced health recalculation for {}", player.getName().getString());
        }

        // For other stats, you might need a similar refresh method in the combat stats attachment
        // For example: player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get()).refreshAllBonuses();
    }

    /**
     * Schedules a high-priority synchronization of all data to the client.
     * This is crucial after a dimension change to ensure the client's view of
     * the player's data is immediately consistent.
     *
     * @param player The player whose data needs to be synchronized.
     */
    public static void scheduleFullSync(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to schedule sync for a null player.");
            return;
        }
        // Schedule a sync with high priority to update the client quickly.
        NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.HIGH);
        ImmortalCultivation.LOGGER.debug("Scheduled a high-priority data sync for {} post-dimension change.", player.getName().getString());
    }
}
