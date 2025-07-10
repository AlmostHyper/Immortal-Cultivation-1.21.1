package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCombatStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerPillConsumptionAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerRaceAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
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
    private static final int SYNC_CHECK_INTERVAL = 4; // Check for syncs 5 times per second

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
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get()));
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_RACE.get()));
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()));
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get()));
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get()));
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get()));
            checkAndSendSyncPacket(player, player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get()));
        }
    }

    private static void checkAndSendSyncPacket(ServerPlayer player, ISyncable syncable) {
        if (syncable.isDirty()) {
            Optional<ISyncPacket> packetOptional = syncable.createPacket(false);
            packetOptional.ifPresent(packet -> {
                ICModPacketRegistry.sendToPlayer(packet, player);
            });
        }
    }
}
