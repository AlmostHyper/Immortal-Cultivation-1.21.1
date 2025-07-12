package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModTickEvent {

    // Base tick intervals for different systems
    private static final int TICKABLE_INTERVAL = 1; // Tick every tick
    private static final int SYNC_CHECK_INTERVAL = 5; // Check for syncs every 5 ticks (4 times per second)
    private static boolean DEBUG_SYNC = false;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }

        // Use a player-specific hash to distribute processing load across different ticks
        int playerOffset = player.getUUID().hashCode();

        // --- Process Tickable Attachments ---
        if ((player.tickCount + playerOffset) % TICKABLE_INTERVAL == 0) {
            player.getExistingData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()).ifPresent(ITickable::tick);
            player.getExistingData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get()).ifPresent(ITickable::tick);
            player.getExistingData(ICModAttachmentTypes.PLAYER_RESOURCES.get()).ifPresent(ITickable::tick);
            player.getExistingData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get()).ifPresent(ITickable::tick);
        }

        // --- Process Syncable Attachments ---
        if ((player.tickCount + playerOffset) % SYNC_CHECK_INTERVAL == 0) {
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Checking sync for player {} at tick {}",
                    player.getName().getString(), player.tickCount);
            }

            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get()), "PlayerTalents");
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_RACE.get()), "PlayerRace");
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()), "PlayerCultivation");
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get()), "PlayerBreakthrough");
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get()), "PlayerResources");
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get()), "PlayerCombatStats");
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get()), "PlayerPillConsumption");
        }
    }

    private static void checkAndSendSyncPacket(ServerPlayer player, ISyncable syncable, String attachmentName) {
        ImmortalCultivation.LOGGER.error("=== checkAndSendSyncPacket called for {} ===", attachmentName);

        if (syncable == null) {
            ImmortalCultivation.LOGGER.warn("Null {} attachment for player {}", attachmentName, player.getName().getString());
            return;
        }

        // FIX: Ensure player reference is set before checking if dirty
        ensurePlayerReferenceSet(syncable, player, attachmentName);

        boolean isDirty = syncable.isDirty();
        ImmortalCultivation.LOGGER.error("{} isDirty: {}", attachmentName, isDirty);

        if (isDirty) {
            try {
                ImmortalCultivation.LOGGER.error("About to call createPacket for {}", attachmentName);
                Optional<ISyncPacket> packetOptional = syncable.createPacket(false);
                ImmortalCultivation.LOGGER.error("createPacket returned: {}", packetOptional.isPresent() ? "PACKET" : "EMPTY");

                if (packetOptional.isPresent()) {
                    ISyncPacket packet = packetOptional.get();
                    ICModPacketRegistry.sendToPlayer(packet, player);
                    syncable.clearDirty();
                    ImmortalCultivation.LOGGER.error("Successfully sent {} packet", attachmentName);
                } else {
                    ImmortalCultivation.LOGGER.error("{} returned empty packet (dirty flag NOT cleared)", attachmentName);
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Exception in checkAndSendSyncPacket for {}: {}", attachmentName, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Add this helper method to ICModTickEvent:
    private static void ensurePlayerReferenceSet(ISyncable syncable, ServerPlayer player, String attachmentName) {
        if (syncable instanceof PlayerTalentsAttachment talents) {
            talents.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerTalentsAttachment");
        } else if (syncable instanceof PlayerResourceAttachment resources) {
            resources.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerResourceAttachment");
        } else if (syncable instanceof PlayerCultivationAttachment cultivation) {
            cultivation.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerCultivationAttachment");
        } else if (syncable instanceof PlayerRaceAttachment race) {
            race.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerRaceAttachment");
        } else if (syncable instanceof PlayerBreakthroughAttachment breakthrough) {
            breakthrough.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerBreakthroughAttachment");
        } else if (syncable instanceof PlayerCombatStatsAttachment combat) {
            combat.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerCombatStatsAttachment");
        } else if (syncable instanceof PlayerPillConsumptionAttachment pills) {
            pills.setPlayer(player);
            ImmortalCultivation.LOGGER.error("Set player reference for PlayerPillConsumptionAttachment");
        }
    }

    // Add method to toggle debug logging
    public static void setDebugSync(boolean debug) {
        DEBUG_SYNC = debug;
        ImmortalCultivation.LOGGER.info("Sync debugging {}", debug ? "enabled" : "disabled");
    }
}
