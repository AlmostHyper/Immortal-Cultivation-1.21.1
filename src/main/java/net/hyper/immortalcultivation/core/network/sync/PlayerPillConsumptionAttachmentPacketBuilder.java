package net.hyper.immortalcultivation.core.network.sync;

import net.hyper.immortalcultivation.common.attachments.PlayerPillConsumptionAttachment;
import net.hyper.immortalcultivation.core.network.packets.server.UnifiedDeltaSyncPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * Builder utility for creating UnifiedDeltaSyncPacket instances specifically for PlayerPillConsumptionAttachment.
 * This simplifies the creation of sync packets when only pill consumption data needs to be synchronized.
 */
public class PlayerPillConsumptionAttachmentPacketBuilder {

    /**
     * Creates a UnifiedDeltaSyncPacket containing only pill consumption data from the provided attachment.
     * All other attachment data fields will be empty.
     *
     * @param pillAttachment The pill consumption attachment to extract data from
     * @return A UnifiedDeltaSyncPacket with only pill consumption data populated
     */
    public static UnifiedDeltaSyncPacket createFullSyncPacket(PlayerPillConsumptionAttachment pillAttachment) {
        CompoundTag pillData = pillAttachment.getPillConsumptionManager().serializeNBT();
        return createSyncPacket(pillData);
    }

    /**
     * Creates a UnifiedDeltaSyncPacket with specific pill consumption data.
     * This is useful when you want to sync specific data rather than extracting from an attachment.
     *
     * @param pillConsumptionData The pill consumption data to sync
     * @return A UnifiedDeltaSyncPacket with the specified pill consumption data
     */
    public static UnifiedDeltaSyncPacket createSyncPacket(CompoundTag pillConsumptionData) {
        // Set pill consumption mask and field mask
        byte attachmentMask = UnifiedDeltaSyncPacket.PILL_CONSUMPTION_MASK;
        byte pillConsumptionFieldMask = UnifiedDeltaSyncPacket.PILL_CONSUMPTION_DATA;

        return new UnifiedDeltaSyncPacket(
            attachmentMask, // Only pill consumption attachment
            (short) 0, (short) 0, (byte) 0, // No other field masks
            (byte) 0, (byte) 0, pillConsumptionFieldMask, // Only pill consumption field mask

            // Cultivation data (all empty)
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Resource data (all empty)
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Combat stats data (all empty)
            Optional.empty(),

            // Damage feedback data (all empty)
            Optional.empty(), Optional.empty(),

            // Race data (all empty)
            Optional.empty(), Optional.empty(),

            // Talent data (all empty)
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Pill consumption data (populated)
            Optional.of(pillConsumptionData)
        );
    }

    /**
     * Creates an empty pill consumption sync packet.
     * This is useful for clearing pill consumption data on the client.
     *
     * @return A UnifiedDeltaSyncPacket with empty pill consumption data
     */
    public static UnifiedDeltaSyncPacket createEmptySyncPacket() {
        return createSyncPacket(new CompoundTag());
    }
}
