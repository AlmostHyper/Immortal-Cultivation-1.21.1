package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.IPlayerInitializable;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

/**
 * A unified event handler for critical player lifecycle events.
 * This class manages the initialization and synchronization of attachments
 * when a player logs in, respawns, or changes dimensions, ensuring data
 * integrity and client-server consistency.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModInitializationEvent {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing login for player: {}", player.getName().getString());
            initializeAttachments(player);
            synchronizeAllData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing respawn for player: {}", player.getName().getString());
            synchronizeAllData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing dimension change for player: {}", player.getName().getString());
            synchronizeAllData(player);
        }
    }

    /**
     * Iterates through all attachments on the player and calls the onPlayerLogin method
     * for any that implement the IPlayerInitializable interface.
     * @param player The player to initialize attachments for.
     */
    private static void initializeAttachments(ServerPlayer player) {
        try {
            player.getAttachments().getValues().stream()
                .filter(IPlayerInitializable.class::isInstance)
                .map(IPlayerInitializable.class::cast)
                .forEach(initializer -> initializer.onPlayerLogin(player));

            ImmortalCultivation.LOGGER.debug("Attachment initialization complete for player: {}",
                player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during attachment initialization for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Synchronizes all ISyncable attachments to the client. This is a full sync,
     * intended to be used on login, respawn, or dimension change.
     * @param player The player to synchronize data for.
     */
    private static void synchronizeAllData(ServerPlayer player) {
        ImmortalCultivation.LOGGER.debug("Starting full data sync for {}", player.getName().getString());

        try {
            player.getAttachments().getValues().stream()
                .filter(ISyncable.class::isInstance)
                .map(ISyncable.class::cast)
                .forEach(syncable -> {
                    Optional<ISyncPacket> packetOptional = syncable.createPacket(true); // true for full sync
                    packetOptional.ifPresent(packet -> ICModPacketRegistry.sendToPlayer(packet, player));
                });

            ImmortalCultivation.LOGGER.debug("Full data sync for {} complete.", player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during full data sync for player {}",
                player.getName().getString(), e);
        }
    }
}
