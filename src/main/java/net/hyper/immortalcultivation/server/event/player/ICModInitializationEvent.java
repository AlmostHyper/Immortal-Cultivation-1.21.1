package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.IPlayerInitializable;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A unified event handler for critical player lifecycle events.
 * This class manages the initialization and synchronization of attachments
 * when a player logs in, respawns, or changes dimensions, ensuring data
 * integrity and client-server consistency.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModInitializationEvent {

    // A list of all attachment type suppliers to be managed.
    // The wildcard `? extends` is used to allow for different specific attachment types.
    private static final List<Supplier<? extends AttachmentType<?>>> ATTACHMENT_TYPES = List.of(
        ICModAttachmentTypes.PLAYER_CULTIVATION,
        ICModAttachmentTypes.PLAYER_BREAKTHROUGH,
        ICModAttachmentTypes.PLAYER_RESOURCES,
        ICModAttachmentTypes.PLAYER_COMBAT_STATS,
        ICModAttachmentTypes.PLAYER_TALENTS,
        ICModAttachmentTypes.PLAYER_RACE,
        ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION
    );

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing login for player: {}", player.getName().getString());
            initializeAttachments(player);

            // Apply current realm modifiers after initialization but before sync
            applyCurrentRealmModifiers(player);

            synchronizeAllData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing respawn for player: {}", player.getName().getString());

            // Reapply realm modifiers on respawn to ensure they persist
            applyCurrentRealmModifiers(player);

            synchronizeAllData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing dimension change for player: {}", player.getName().getString());

            // Reapply realm modifiers on dimension change to ensure they persist
            applyCurrentRealmModifiers(player);

            synchronizeAllData(player);
        }
    }

    /**
     * Applies current realm modifiers for all cultivation types.
     * This recalculates from scratch to ensure accuracy and handle any data inconsistencies.
     */
    private static void applyCurrentRealmModifiers(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                ImmortalCultivation.LOGGER.warn("No cultivation attachment found for player {}", player.getName().getString());
                return;
            }

            // Recalculate all bonuses from scratch on login to ensure accuracy
            // This handles cases where data might be out of sync
            for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
                CultivationRealm currentRealm = cultivation.getRealm(type);
                if (currentRealm != null && currentRealm.getRealmLevel() > 0) {
                    cultivation.recalculateAllRealmBonuses(player, type);
                    ImmortalCultivation.LOGGER.debug("Recalculated {} realm bonuses for {} (current realm: {})",
                        type.getDisplayName(), player.getName().getString(), currentRealm.getDisplayName());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying realm modifiers for player {} during lifecycle event",
                player.getName().getString(), e);
        }
    }

    private static Stream<Object> getAttachments(ServerPlayer player) {
        return ATTACHMENT_TYPES.stream().map(supplier -> player.getData(supplier.get()));
    }

    /**
     * Iterates through all attachments on the player and calls the onPlayerLogin method
     * for any that implement the IPlayerInitializable interface.
     * @param player The player to initialize attachments for.
     */
    private static void initializeAttachments(ServerPlayer player) {
        try {
            getAttachments(player)
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
            getAttachments(player)
                .filter(ISyncable.class::isInstance)
                .map(ISyncable.class::cast)
                .forEach(syncable -> {
                    // FIX: Ensure player reference is set before creating packet
                    ensurePlayerReferenceSet(syncable, player);

                    Optional<ISyncPacket> packetOptional = syncable.createPacket(true); // true for full sync
                    packetOptional.ifPresent(packet -> ICModPacketRegistry.sendToPlayer(packet, player));
                });

            ImmortalCultivation.LOGGER.debug("Full data sync for {} complete.", player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during full data sync for player {}",
                player.getName().getString(), e);
        }
    }

    // Add this helper method to ICModInitializationEvent:
    private static void ensurePlayerReferenceSet(ISyncable syncable, ServerPlayer player) {
        if (syncable instanceof PlayerTalentsAttachment talents) {
            talents.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerTalentsAttachment");
        } else if (syncable instanceof PlayerResourceAttachment resources) {
            resources.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerResourceAttachment");
        } else if (syncable instanceof PlayerCultivationAttachment cultivation) {
            cultivation.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerCultivationAttachment");
        } else if (syncable instanceof PlayerRaceAttachment race) {
            race.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerRaceAttachment");
        } else if (syncable instanceof PlayerBreakthroughAttachment breakthrough) {
            breakthrough.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerBreakthroughAttachment");
        } else if (syncable instanceof PlayerCombatStatsAttachment combat) {
            combat.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerCombatStatsAttachment");
        } else if (syncable instanceof PlayerPillConsumptionAttachment pills) {
            pills.setPlayer(player);
            ImmortalCultivation.LOGGER.error("INIT: Set player reference for PlayerPillConsumptionAttachment");
        }
    }
}