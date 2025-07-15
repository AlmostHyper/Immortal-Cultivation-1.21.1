package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerLifecycleEventHandler {

    /**
     * Handles player login initialization and synchronization
     *
     * @param event Player login event
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Initializing player data for: {}", player.getName().getString());

            initializePlayerAttachments(player);
            applyCurrentRealmModifiers(player);
            synchronizeAllPlayerData(player);

            ImmortalCultivation.LOGGER.debug("Player {} fully initialized", player.getName().getString());
        }
    }

    /**
     * Handles player respawn events - reapplies realm modifiers and restores stats to full
     *
     * @param event Player respawn event
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing respawn for: {}", player.getName().getString());

            // Clear immunity tracking on respawn
            clearPlayerImmunity(player.getUUID());

            // FIRST: Reapply realm modifiers to set up proper max stats
            applyCurrentRealmModifiers(player);

            // SECOND: Get player stats attachment AFTER realm modifiers are applied
            PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
            if (statsAttachment != null) {
                // Restore all stats to their proper maximum values
                restoreStatsToFull(player, statsAttachment);

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("Restored stats to full for {}: H:{}/{} Q:{}/{} B:{}/{}",
                        player.getName().getString(),
                        statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                        statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                        statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                        statsAttachment.getMaxStat(IStatsData.StatType.QI),
                        statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                        statsAttachment.getMaxStat(IStatsData.StatType.BODY));
                }
            }

            // THIRD: Synchronize all player data
            synchronizeAllPlayerData(player);
        }
    }

    /**
     * Handles dimension change events
     *
     * @param event Player dimension change event
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing dimension change for: {}", player.getName().getString());

            applyCurrentRealmModifiers(player);
            synchronizeAllPlayerData(player);
        }
    }
}
