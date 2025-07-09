package net.hyper.immortalcultivation.core.network.sync;

import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.network.packets.server.UnifiedDeltaSyncPacket;

import java.util.Optional;

/**
 * Builder utility for creating UnifiedDeltaSyncPacket instances specifically for PlayerTalentAttachment.
 * This simplifies the creation of sync packets when only talent data needs to be synchronized.
 */
public class PlayerTalentAttachmentPacketBuilder {

    /**
     * Creates a UnifiedDeltaSyncPacket containing only talent data from the provided attachment.
     * All other attachment data fields will be empty.
     *
     * @param talentAttachment The talent attachment to extract data from
     * @return A UnifiedDeltaSyncPacket with only talent data populated
     */
    public static UnifiedDeltaSyncPacket createFullSyncPacket(PlayerTalentAttachment talentAttachment) {
        return createSyncPacket(
            talentAttachment.getSpiritualRootType(),
            talentAttachment.hasSpiritualRootAssigned(),
            talentAttachment.getPhysiqueType(),
            talentAttachment.hasPhysiqueAssigned()
        );
    }

    /**
     * Creates a UnifiedDeltaSyncPacket with specific talent data.
     * This is useful when you want to sync specific values rather than extracting from an attachment.
     *
     * @param spiritualRootType The spiritual root type to sync
     * @param spiritualRootAssigned Whether the spiritual root is assigned
     * @param physiqueType The physique type to sync
     * @param physiqueAssigned Whether the physique is assigned
     * @return A UnifiedDeltaSyncPacket with the specified talent data
     */
    public static UnifiedDeltaSyncPacket createSyncPacket(SpiritualRootType spiritualRootType,
                                                          boolean spiritualRootAssigned,
                                                          PhysiqueType physiqueType,
                                                          boolean physiqueAssigned) {
        // Set talent mask and all talent field masks
        byte attachmentMask = UnifiedDeltaSyncPacket.TALENT_MASK;
        byte talentFieldMask = UnifiedDeltaSyncPacket.TALENT_ALL;

        return new UnifiedDeltaSyncPacket(
            attachmentMask, // Only talent attachment
            (short) 0, (short) 0, (byte) 0, // No other field masks
            (byte) 0, talentFieldMask, (byte) 0, // Only talent field mask

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

            // Talent data (populated)
            Optional.of(spiritualRootType.name()),
            Optional.of(spiritualRootAssigned),
            Optional.of(physiqueType.name()),
            Optional.of(physiqueAssigned),

            // Pill consumption data (empty)
            Optional.empty()
        );
    }

    /**
     * Creates a delta sync packet for only spiritual root changes.
     * This is useful when only the spiritual root has changed and you want to minimize network traffic.
     *
     * @param spiritualRootType The new spiritual root type
     * @param spiritualRootAssigned Whether the spiritual root is assigned
     * @return A UnifiedDeltaSyncPacket with only spiritual root data
     */
    public static UnifiedDeltaSyncPacket createSpiritualRootDeltaPacket(SpiritualRootType spiritualRootType,
                                                                        boolean spiritualRootAssigned) {
        byte attachmentMask = UnifiedDeltaSyncPacket.TALENT_MASK;
        byte talentFieldMask = UnifiedDeltaSyncPacket.TALENT_SPIRITUAL_ROOT_TYPE |
            UnifiedDeltaSyncPacket.TALENT_SPIRITUAL_ROOT_ASSIGNED;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, (short) 0, (byte) 0,
            (byte) 0, talentFieldMask, (byte) 0,

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // All damage feedback data empty
            Optional.empty(), Optional.empty(),

            // All race data empty
            Optional.empty(), Optional.empty(),

            // Only spiritual root talent data
            Optional.of(spiritualRootType.name()),
            Optional.of(spiritualRootAssigned),
            Optional.empty(), // No physique type
            Optional.empty(), // No physique assigned

            // No pill consumption data
            Optional.empty()
        );
    }

    /**
     * Creates a delta sync packet for only physique changes.
     * This is useful when only the physique has changed and you want to minimize network traffic.
     *
     * @param physiqueType The new physique type
     * @param physiqueAssigned Whether the physique is assigned
     * @return A UnifiedDeltaSyncPacket with only physique data
     */
    public static UnifiedDeltaSyncPacket createPhysiqueDeltaPacket(PhysiqueType physiqueType,
                                                                   boolean physiqueAssigned) {
        byte attachmentMask = UnifiedDeltaSyncPacket.TALENT_MASK;
        byte talentFieldMask = UnifiedDeltaSyncPacket.TALENT_PHYSIQUE_TYPE |
            UnifiedDeltaSyncPacket.TALENT_PHYSIQUE_ASSIGNED;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, (short) 0, (byte) 0,
            (byte) 0, talentFieldMask, (byte) 0,

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // All damage feedback data empty
            Optional.empty(), Optional.empty(),

            // All race data empty
            Optional.empty(), Optional.empty(),

            // Only physique talent data
            Optional.empty(), // No spiritual root type
            Optional.empty(), // No spiritual root assigned
            Optional.of(physiqueType.name()),
            Optional.of(physiqueAssigned),

            // No pill consumption data
            Optional.empty()
        );
    }
}
