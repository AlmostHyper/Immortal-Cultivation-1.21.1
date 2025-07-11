package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NetworkSyncManager {

    public static final NetworkSyncManager INSTANCE = new NetworkSyncManager();
    private static boolean DEBUG_SYNC = true; // Toggle for sync debugging

    private final ConcurrentHashMap<UUID, SyncRequest> pendingSyncs = new ConcurrentHashMap<>();

    private static class SyncRequest {
        final ServerPlayer player;
        final Supplier<AttachmentType<? extends ISyncable>> attachmentType;
        boolean fullSync;
        final String attachmentName; // For logging

        SyncRequest(ServerPlayer player, Supplier<AttachmentType<? extends ISyncable>> attachmentType, boolean fullSync) {
            this.player = player;
            this.attachmentType = attachmentType;
            this.fullSync = fullSync;
            // Extract attachment name for logging
            this.attachmentName = attachmentType.get().toString()
                .replaceAll(".*:", "")
                .replaceAll("\\[.*", "");
        }

        void merge(boolean isFullSync) {
            this.fullSync = this.fullSync || isFullSync;
        }
    }

    public void scheduleSync(ServerPlayer player, Supplier<AttachmentType<? extends ISyncable>> attachmentType, boolean fullSync) {
        String attachmentName = attachmentType.get().toString()
            .replaceAll(".*:", "")
            .replaceAll("\\[.*", "");

        if (DEBUG_SYNC) {
            ImmortalCultivation.LOGGER.debug("Scheduling {} sync for player {} (fullSync: {})",
                attachmentName, player.getName().getString(), fullSync);
        }

        pendingSyncs.merge(player.getUUID(),
            new SyncRequest(player, attachmentType, fullSync),
            (existing, newRequest) -> {
                existing.merge(fullSync);
                if (DEBUG_SYNC && existing.fullSync != fullSync) {
                    ImmortalCultivation.LOGGER.debug("Upgraded {} sync to fullSync for player {}",
                        attachmentName, player.getName().getString());
                }
                return existing;
            });
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        processSyncs();
    }

    private void processSyncs() {
        if (pendingSyncs.isEmpty()) {
            return;
        }

        if (DEBUG_SYNC) {
            ImmortalCultivation.LOGGER.debug("Processing {} pending sync requests", pendingSyncs.size());
        }

        int successCount = 0;
        int failureCount = 0;

        for (SyncRequest request : pendingSyncs.values()) {
            try {
                // Check if player is still valid
                if (request.player == null || !request.player.isAlive()) {
                    if (DEBUG_SYNC) {
                        ImmortalCultivation.LOGGER.warn("Skipping sync for invalid/dead player");
                    }
                    failureCount++;
                    continue;
                }

                ISyncable syncable = request.player.getData(request.attachmentType.get());
                if (syncable != null) {
                    Optional<ISyncPacket> packet = syncable.createPacket(request.fullSync);

                    if (packet.isPresent()) {
                        ICModPacketRegistry.sendToPlayer(packet.get(), request.player);
                        syncable.clearDirty();
                        successCount++;

                        if (DEBUG_SYNC) {
                            ImmortalCultivation.LOGGER.debug("Successfully sent {} sync to player {} (fullSync: {})",
                                request.attachmentName, request.player.getName().getString(), request.fullSync);
                        }
                    } else {
                        failureCount++;
                        if (DEBUG_SYNC) {
                            ImmortalCultivation.LOGGER.debug("No packet created for {} sync (player: {}, fullSync: {})",
                                request.attachmentName, request.player.getName().getString(), request.fullSync);
                        }
                    }
                } else {
                    failureCount++;
                    if (DEBUG_SYNC) {
                        ImmortalCultivation.LOGGER.warn("Null {} attachment for player {}",
                            request.attachmentName, request.player.getName().getString());
                    }
                }
            } catch (Exception e) {
                failureCount++;
                ImmortalCultivation.LOGGER.error("Error processing sync for player {}: {}",
                    request.player != null ? request.player.getName().getString() : "null",
                    e.getMessage());
                e.printStackTrace();
            }
        }

        if (DEBUG_SYNC && (successCount > 0 || failureCount > 0)) {
            ImmortalCultivation.LOGGER.debug("Sync batch complete: {} successful, {} failed",
                successCount, failureCount);
        }

        pendingSyncs.clear();
    }

    // Add method to toggle debug logging
    public static void setDebugSync(boolean debug) {
        DEBUG_SYNC = debug;
        ImmortalCultivation.LOGGER.info("NetworkSyncManager debugging {}", debug ? "enabled" : "disabled");
    }

    // Add method to check pending syncs (for debugging)
    public int getPendingSyncCount() {
        return pendingSyncs.size();
    }
}