package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@EventBusSubscriber
public class NetworkSyncManager {

    public static final NetworkSyncManager INSTANCE = new NetworkSyncManager();

    private final ConcurrentHashMap<UUID, SyncRequest> pendingSyncs = new ConcurrentHashMap<>();

    private static class SyncRequest {
        final ServerPlayer player;
        final Supplier<AttachmentType<? extends ISyncable>> attachmentType;
        boolean fullSync;

        SyncRequest(ServerPlayer player, Supplier<AttachmentType<? extends ISyncable>> attachmentType, boolean fullSync) {
            this.player = player;
            this.attachmentType = attachmentType;
            this.fullSync = fullSync;
        }

        void merge(boolean isFullSync) {
            this.fullSync = this.fullSync || isFullSync;
        }
    }

    public void scheduleSync(ServerPlayer player, Supplier<AttachmentType<? extends ISyncable>> attachmentType, boolean fullSync) {
        pendingSyncs.merge(player.getUUID(), new SyncRequest(player, attachmentType, fullSync),
                (existing, a) -> {
                    existing.merge(fullSync);
                    return existing;
                });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        INSTANCE.processSyncs();
    }

    private void processSyncs() {
        if (pendingSyncs.isEmpty()) {
            return;
        }

        for (SyncRequest request : pendingSyncs.values()) {
            ISyncable syncable = request.player.getData(request.attachmentType.get());
            if (syncable != null) {
                Optional<ISyncPacket> packet = syncable.createPacket(request.fullSync);
                packet.ifPresent(p -> {
                    ICModPacketRegistry.sendToPlayer(p, request.player);
                    syncable.clearDirty();
                });
            }
        }
        pendingSyncs.clear();
    }
}
