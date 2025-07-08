package net.hyper.immortalcultivation.common.capability.attachments;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.item.pills.PillConsumptionManager;
import net.hyper.immortalcultivation.core.network.packets.server.PillConsumptionSyncPacket;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Dedicated attachment for tracking pill consumption and diminishing returns.
 * Separated from PlayerCultivationAttachment for better performance - only syncs when pills are consumed.
 * Enhanced with proper dimension change handling.
 */
public class PlayerPillConsumptionAttachment {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Pill Consumption Tracking ===
    private final PillConsumptionManager pillConsumptionManager;

    // === CONSTRUCTORS ===

    public PlayerPillConsumptionAttachment() {
        this.pillConsumptionManager = new PillConsumptionManager();
    }

    // Constructor for CODEC deserialization
    private PlayerPillConsumptionAttachment(PillConsumptionManager pillConsumptionManager) {
        this.pillConsumptionManager = pillConsumptionManager != null ? pillConsumptionManager : new PillConsumptionManager();
    }

    // === PLAYER REFERENCE MANAGEMENT ===

    /**
     * CRITICAL: Set player reference for sync and calculation purposes.
     * Must be called after dimension changes and during initialization.
     */
    public void setPlayer(Player player) {
        this.player = player;
        ImmortalCultivation.LOGGER.debug("Set player reference for {}: {}",
            this.getClass().getSimpleName(),
            player != null ? player.getName().getString() : "null");
    }

    @Nullable
    public Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === PILL CONSUMPTION METHODS ===

    /**
     * Get the pill consumption manager for this player.
     * This allows access to diminishing returns functionality.
     *
     * @return The pill consumption manager instance
     */
    public PillConsumptionManager getPillConsumptionManager() {
        return pillConsumptionManager;
    }

    /**
     * Apply sync data from server (called by sync packet)
     */
    public void applySync(CompoundTag pillConsumptionData) {
        if (pillConsumptionData != null && !pillConsumptionData.isEmpty()) {
            this.pillConsumptionManager.deserializeNBT(pillConsumptionData);

            ImmortalCultivation.LOGGER.debug("Applied pill consumption sync data from server for player: {}",
                player != null ? player.getName().getString() : "unknown");
        } else {
            ImmortalCultivation.LOGGER.debug("Received empty pill consumption sync data");
        }
    }

    // === SYNC MANAGEMENT ===

    /**
     * CRITICAL: Force sync method for dimension changes and initialization.
     * This ensures the client receives updated data even if internal state hasn't changed.
     */
    public void forceSync() {
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                CompoundTag data = pillConsumptionManager.serializeNBT();
                PillConsumptionSyncPacket packet = new PillConsumptionSyncPacket(data);

                ICModPacketRegistry.sendToPlayer(packet, serverPlayer);

                ImmortalCultivation.LOGGER.debug("Force synced {} for player: {}",
                    this.getClass().getSimpleName(),
                    serverPlayer.getName().getString());
            } else {
                ImmortalCultivation.LOGGER.debug("Cannot sync pill consumption data - no player reference or not server player");
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing {} for player {}",
                this.getClass().getSimpleName(),
                player != null ? player.getName().getString() : "unknown", e);
        }
    }

    // === UTILITY METHODS ===

    /**
     * Create a copy of this attachment (useful for certain attachment operations)
     */
    public PlayerPillConsumptionAttachment copy() {
        PlayerPillConsumptionAttachment copy = new PlayerPillConsumptionAttachment();

        // Copy pill consumption data
        CompoundTag data = this.pillConsumptionManager.serializeNBT();
        if (data != null && !data.isEmpty()) {
            copy.pillConsumptionManager.deserializeNBT(data);
        }

        return copy;
    }

    /**
     * Serialize this attachment to CompoundTag (for manual serialization if needed)
     */
    public CompoundTag serialize() {
        return pillConsumptionManager.serializeNBT();
    }

    /**
     * Deserialize from CompoundTag (for manual deserialization if needed)
     */
    public void deserialize(CompoundTag tag) {
        if (tag != null && !tag.isEmpty()) {
            pillConsumptionManager.deserializeNBT(tag);
        }
    }

    /**
     * Reset all pill consumption data (for debug/admin purposes)
     */
    public void resetPillConsumption() {
        ImmortalCultivation.LOGGER.warn("Resetting pill consumption data for player: {}",
            player != null ? player.getName().getString() : "Unknown");

        // Create a new manager to clear all data
        CompoundTag emptyTag = new CompoundTag();
        pillConsumptionManager.deserializeNBT(emptyTag);

        forceSync();
    }

    /**
     * Get debug information about pill consumption
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Pill Consumption Debug Info ===\n");
        info.append("Player: ").append(player != null ? player.getName().getString() : "Unknown").append("\n");
        info.append("Side: ").append(player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown").append("\n");

        // Add pill consumption manager specific debug info if available
        CompoundTag data = pillConsumptionManager.serializeNBT();
        info.append("Data entries: ").append(data.size()).append("\n");
        info.append("=================================");

        return info.toString();
    }

    public void debugInfo() {
        ImmortalCultivation.LOGGER.info(getDebugInfo());
    }

    // === SERIALIZATION ===

    public static final Codec<PlayerPillConsumptionAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            // Use the PillConsumptionManager's CODEC if it exists, otherwise use CompoundTag
            CompoundTag.CODEC.optionalFieldOf("pill_consumption", new CompoundTag())
                .forGetter(attachment -> attachment.pillConsumptionManager.serializeNBT())
        ).apply(instance, PlayerPillConsumptionAttachment::fromNBT)
    );

    /**
     * Create instance from NBT data for CODEC
     */
    private static PlayerPillConsumptionAttachment fromNBT(CompoundTag pillConsumptionData) {
        PlayerPillConsumptionAttachment attachment = new PlayerPillConsumptionAttachment();

        // Restore pill consumption data if present
        if (pillConsumptionData != null && !pillConsumptionData.isEmpty()) {
            attachment.pillConsumptionManager.deserializeNBT(pillConsumptionData);
        }

        return attachment;
    }

    // === SERIALIZER ===

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerPillConsumptionAttachment> {
        @Override
        public PlayerPillConsumptionAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerPillConsumptionAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerPillConsumptionAttachment: {}", error))
                .map(pair -> pair.getFirst())
                .orElse(new PlayerPillConsumptionAttachment());

            // CRITICAL: Set player reference during deserialization
            if (holder instanceof Player player) {
                attachment.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Set player reference during pill consumption attachment deserialization: {}",
                    player.getName().getString());
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerPillConsumptionAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encode(attachment, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerPillConsumptionAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }
}