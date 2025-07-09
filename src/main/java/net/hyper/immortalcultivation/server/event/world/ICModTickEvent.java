package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
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
            player.getAttachments().getValues().stream()
                  .filter(attachment -> attachment instanceof ITickable)
                  .forEach(attachment -> ((ITickable) attachment).tick());
        }

        // --- Process Syncable Attachments ---
        if ((player.tickCount + playerOffset) % SYNC_CHECK_INTERVAL == 0) {
            player.getAttachments().getValues().stream()
                  .filter(attachment -> attachment instanceof ISyncable)
                  .map(attachment -> (ISyncable) attachment)
                  .filter(ISyncable::isDirty)
                  .forEach(syncable -> {
                      // Create a sync packet for the dirty attachment. 'false' indicates this is not a full-sync request.
                      Optional<ISyncPacket> packetOptional = syncable.createPacket(false);
                      packetOptional.ifPresent(packet -> {
                          // Send the specific packet for this attachment directly to the player
                          ICModPacketRegistry.sendToPlayer(packet, player);
                      });
                  });
        }
    }
}
