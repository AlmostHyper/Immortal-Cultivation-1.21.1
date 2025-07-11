package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
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
    private static boolean DEBUG_SYNC = true;              // Toggle for sync debugging

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
        if (syncable == null) {
            ImmortalCultivation.LOGGER.warn("Null {} attachment for player {}",
                attachmentName, player.getName().getString());
            return;
        }

        boolean isDirty = syncable.isDirty();

        if (DEBUG_SYNC && isDirty) {
            ImmortalCultivation.LOGGER.debug("{} attachment is dirty for player {}",
                attachmentName, player.getName().getString());
        }

        if (isDirty) {
            try {
                Optional<ISyncPacket> packetOptional = syncable.createPacket(false);

                if (packetOptional.isPresent()) {
                    ISyncPacket packet = packetOptional.get();
                    ICModPacketRegistry.sendToPlayer(packet, player);
                    syncable.clearDirty(); // Clear dirty flag only after successful send

                    if (DEBUG_SYNC) {
                        ImmortalCultivation.LOGGER.debug("Successfully sent {} sync packet to player {}",
                            attachmentName, player.getName().getString());
                    }
                } else if (DEBUG_SYNC) {
                    ImmortalCultivation.LOGGER.debug("{} attachment returned empty packet for player {} (dirty flag NOT cleared)",
                        attachmentName, player.getName().getString());
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error creating/sending {} packet for player {}: {}",
                    attachmentName, player.getName().getString(), e.getMessage());
                e.printStackTrace();
                // Don't clear dirty flag on error
            }
        }
    }

    // Add method to toggle debug logging
    public static void setDebugSync(boolean debug) {
        DEBUG_SYNC = debug;
        ImmortalCultivation.LOGGER.info("Sync debugging {}", debug ? "enabled" : "disabled");
    }
}
