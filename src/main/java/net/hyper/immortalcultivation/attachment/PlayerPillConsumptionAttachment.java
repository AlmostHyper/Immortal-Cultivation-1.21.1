package net.hyper.immortalcultivation.attachment;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.item.pills.PillConsumptionManager;
import net.hyper.immortalcultivation.network.server.PillConsumptionSyncPacket;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Dedicated attachment for tracking pill consumption and diminishing returns.
 * Separated from PlayerCultivationAttachment for better performance - only syncs when pills are consumed.
 */
public class PlayerPillConsumptionAttachment {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Pill Consumption Tracking ===
    private final PillConsumptionManager pillConsumptionManager;

    // === Constructors ===
    public PlayerPillConsumptionAttachment() {
        this.pillConsumptionManager = new PillConsumptionManager();
    }

    // Constructor for CODEC deserialization
    private PlayerPillConsumptionAttachment(PillConsumptionManager pillConsumptionManager) {
        this.pillConsumptionManager = pillConsumptionManager != null ? pillConsumptionManager : new PillConsumptionManager();
    }

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
     * Set the player reference for sync purposes
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Force sync pill consumption data to client.
     * Call this after consuming pills to update tooltips.
     */
    public void forceSync() {
        if (player instanceof ServerPlayer serverPlayer) {
            PillConsumptionSyncPacket packet = new PillConsumptionSyncPacket(
                pillConsumptionManager.serializeNBT()
            );

            ICModNetwork.sendToPlayer(packet, serverPlayer);

            ImmortalCultivation.LOGGER.debug("Synced pill consumption data to client for player: {}",
                player.getName().getString());
        }
    }

    /**
     * Apply sync data from server
     */
    public void applySync(CompoundTag pillConsumptionData) {
        if (pillConsumptionData != null && !pillConsumptionData.isEmpty()) {
            this.pillConsumptionManager.deserializeNBT(pillConsumptionData);

            ImmortalCultivation.LOGGER.debug("Applied pill consumption sync data from server");
        }
    }

    // === Serialization with Codec ===

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
}