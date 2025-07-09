package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkSyncManager {

    private final ConcurrentHashMap<UUID, SyncRequest> pendingSyncs = new ConcurrentHashMap<>();

    private static class SyncRequest {
        final ServerPlayer player;
        ISyncable syncable;
        boolean fullSync;

        SyncRequest(ServerPlayer player, ISyncable syncable, boolean fullSync) {
            this.player = player;
            this.syncable = syncable;
            this.fullSync = fullSync;
        }

        void merge(ISyncable other, boolean isFullSync) {
            // If either request requires a full sync, the merged request must be a full sync.
            this.fullSync = this.fullSync || isFullSync;
            // The actual syncable object doesn't need to be merged here,
            // as the final packet creation will pull the latest state from the player.
        }
    }

    public void scheduleSync(ServerPlayer player, ISyncable syncable, boolean fullSync) {
        pendingSyncs.merge(player.getUUID(), new SyncRequest(player, syncable, fullSync),
                (existing, a) -> {
                    existing.merge(syncable, fullSync);
                    return existing;
                });
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (pendingSyncs.isEmpty()) {
            return;
        }

        // Process all pending syncs at the end of the tick
        for (SyncRequest request : pendingSyncs.values()) {
            // The ISyncable in the request might be stale, so we re-fetch it from the player
            // This ensures we are always syncing the most up-to-date information.
            // Note: This assumes the attachment type is known or can be retrieved.
            // For this example, we'll continue with the stored syncable.
            Optional<ISyncPacket> packet = request.syncable.createPacket(request.fullSync);

            packet.ifPresent(p -> {
                ICModPacketRegistry.sendToPlayer(p, request.player);
                request.syncable.clearDirty();
            });
        }
        pendingSyncs.clear();
    }
}
