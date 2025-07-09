package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
        // Use a simple modulo check with the player offset to spread the load
        if ((player.tickCount + playerOffset) % TICKABLE_INTERVAL == 0) {
            player.getAttachments().getValues().stream()
                  .filter(attachment -> attachment instanceof ITickable)
                  .forEach(attachment -> ((ITickable) attachment).tick());
        }

        // --- Process Syncable Attachments ---
        // Check for dirty attachments at a different interval to reduce overhead
        if ((player.tickCount + playerOffset) % SYNC_CHECK_INTERVAL == 0) {
            player.getAttachments().getValues().stream()
                  .filter(attachment -> attachment instanceof ISyncable)
                  .map(attachment -> (ISyncable) attachment)
                  .filter(ISyncable::isDirty)
                  .forEach(syncable -> {
                      // Schedule the sync with the manager. The manager will handle batching and sending.
                      // We pass 'false' for fullSync, as this is a routine delta-sync check.
                      ImmortalCultivation.SYNC_MANAGER.scheduleSync(player, syncable, false);
                  });
        }
    }
}
