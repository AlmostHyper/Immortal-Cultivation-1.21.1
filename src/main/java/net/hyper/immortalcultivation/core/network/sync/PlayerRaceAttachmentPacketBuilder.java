package net.hyper.immortalcultivation.core.network.sync;

import net.hyper.immortalcultivation.common.attachments.PlayerRaceAttachment;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.packets.server.UnifiedDeltaSyncPacket;

import java.util.Optional;
/**
 * Builder utility for creating UnifiedDeltaSyncPacket instances specifically for PlayerRaceAttachment.
 * This simplifies the creation of sync packets when only race data needs to be synchronized.
 */
public class PlayerRaceAttachmentPacketBuilder {

    /**
     * Creates a UnifiedDeltaSyncPacket containing only race data from the provided attachment.
     * All other attachment data fields will be empty.
     *
     * @param raceAttachment The race attachment to extract data from
     * @return A UnifiedDeltaSyncPacket with only race data populated
     */
    public static UnifiedDeltaSyncPacket createFullSyncPacket(PlayerRaceAttachment raceAttachment) {
        return createSyncPacket(
            raceAttachment.getRaceType(),
            raceAttachment.hasRaceAssigned()
        );
    }

    /**
     * Creates a UnifiedDeltaSyncPacket with specific race data.
     * This is useful when you want to sync specific values rather than extracting from an attachment.
     *
     * @param raceType The race type to sync
     * @param raceAssigned Whether the race is assigned
     * @return A UnifiedDeltaSyncPacket with the specified race data
     */
    public static UnifiedDeltaSyncPacket createSyncPacket(RaceType raceType, boolean raceAssigned) {
        // Set race mask and all race field masks
        byte attachmentMask = UnifiedDeltaSyncPacket.RACE_MASK;
        byte raceFieldMask = UnifiedDeltaSyncPacket.RACE_ALL;

        return new UnifiedDeltaSyncPacket(
            attachmentMask, // Only race attachment
            (short) 0, (short) 0, (byte) 0, // No other field masks
            raceFieldMask, (byte) 0, (byte) 0, // Only race field mask

            // Cultivation data (all empty)
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Resource data (all empty)
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Combat stats data (all empty)
            Optional.empty(),

            // Damage feedback data (all empty)
            Optional.empty(), Optional.empty(),

            // Race data (populated)
            Optional.of(raceType.name()),
            Optional.of(raceAssigned),

            // Talent data (all empty)
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Pill consumption data (empty)
            Optional.empty()
        );
    }

    /**
     * Creates a delta sync packet for only race type changes.
     * This is useful when only the race type has changed and you want to minimize network traffic.
     *
     * @param raceType The new race type
     * @return A UnifiedDeltaSyncPacket with only race type data
     */
    public static UnifiedDeltaSyncPacket createRaceTypeDeltaPacket(RaceType raceType) {
        byte attachmentMask = UnifiedDeltaSyncPacket.RACE_MASK;
        byte raceFieldMask = UnifiedDeltaSyncPacket.RACE_TYPE;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, (short) 0, (byte) 0,
            raceFieldMask, (byte) 0, (byte) 0,

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // All damage feedback data empty
            Optional.empty(), Optional.empty(),

            // Only race type
            Optional.of(raceType.name()),
            Optional.empty(), // No race assigned flag

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }

    /**
     * Creates a delta sync packet for only race assignment status changes.
     * This is useful when only the assignment status has changed.
     *
     * @param raceAssigned Whether the race is assigned
     * @return A UnifiedDeltaSyncPacket with only race assignment data
     */
    public static UnifiedDeltaSyncPacket createRaceAssignmentDeltaPacket(boolean raceAssigned) {
        byte attachmentMask = UnifiedDeltaSyncPacket.RACE_MASK;
        byte raceFieldMask = UnifiedDeltaSyncPacket.RACE_ASSIGNED;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, (short) 0, (byte) 0,
            raceFieldMask, (byte) 0, (byte) 0,

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // All damage feedback data empty
            Optional.empty(), Optional.empty(),

            // Only race assignment
            Optional.empty(), // No race type
            Optional.of(raceAssigned),

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }
}
