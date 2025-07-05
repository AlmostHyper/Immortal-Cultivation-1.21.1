package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.network.server.TalentSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Unified talent data attachment.
 * Merges: PlayerSpiritualRootAttachment and PlayerPhysiqueAttachment
 * Enhanced with proper dimension change handling.
 */
public class PlayerTalentAttachment {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Spiritual Root Data ===
    private SpiritualRootType spiritualRootType = SpiritualRootType.MORTAL_ROOT;
    private boolean spiritualRootAssigned = false;

    // === Physique Data ===
    private PhysiqueType physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
    private boolean physiqueAssigned = false;

    // === Sync Management ===
    private boolean needsSync = false;

    // === CONSTRUCTORS ===
    public PlayerTalentAttachment() {}

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
    protected Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === SPIRITUAL ROOT METHODS ===

    public SpiritualRootType getSpiritualRootType() {
        return spiritualRootType;
    }

    public void assignSpiritualRoot(SpiritualRootType type) {
        if (!spiritualRootAssigned) {
            this.spiritualRootType = type;
            this.spiritualRootAssigned = true;
            this.needsSync = true;

            ImmortalCultivation.LOGGER.info("Player {} assigned spiritual root: {} ({}x multiplier)",
                player != null ? player.getName().getString() : "Unknown",
                type.getDisplayName(),
                type.getCultivationMultiplier());

            forceSync();
        } else {
            ImmortalCultivation.LOGGER.warn("Attempted to assign spiritual root to player {} who already has one assigned: {}",
                player != null ? player.getName().getString() : "Unknown",
                spiritualRootType.getDisplayName());
        }
    }

    public boolean hasSpiritualRootAssigned() {
        return spiritualRootAssigned;
    }

    public float getSpiritualRootMultiplier() {
        return spiritualRootType.getCultivationMultiplier();
    }

    public String getSpiritualRootDisplayName() {
        return spiritualRootType.getDisplayName();
    }

    public int getSpiritualRootColor() {
        return spiritualRootType.getColor();
    }

    // === PHYSIQUE METHODS ===

    public PhysiqueType getPhysiqueType() {
        return physiqueType;
    }

    public void assignPhysique(PhysiqueType type) {
        if (!physiqueAssigned) {
            this.physiqueType = type;
            this.physiqueAssigned = true;
            this.needsSync = true;

            ImmortalCultivation.LOGGER.info("Player {} assigned physique: {} ({}x multiplier)",
                player != null ? player.getName().getString() : "Unknown",
                type.getDisplayName(),
                type.getCultivationMultiplier());

            forceSync();
        } else {
            ImmortalCultivation.LOGGER.warn("Attempted to assign physique to player {} who already has one assigned: {}",
                player != null ? player.getName().getString() : "Unknown",
                physiqueType.getDisplayName());
        }
    }

    public boolean hasPhysiqueAssigned() {
        return physiqueAssigned;
    }

    public float getPhysiqueMultiplier() {
        return physiqueType.getCultivationMultiplier();
    }

    public String getPhysiqueDisplayName() {
        return physiqueType.getDisplayName();
    }

    public int getPhysiqueColor() {
        return physiqueType.getColor();
    }

    // === COMBINED TALENT METHODS ===

    /**
     * Check if player has achieved Divine Dao Destiny (both heavenly talents)
     */
    public boolean hasDivineDaoDestiny() {
        return spiritualRootType.getCultivationMultiplier() == 1.5f &&
            physiqueType.getCultivationMultiplier() == 1.5f;
    }

    /**
     * Check if player has any legendary+ talent
     */
    public boolean hasLegendaryTalent() {
        return spiritualRootType.getCultivationMultiplier() >= 1.4f ||
            physiqueType.getCultivationMultiplier() >= 1.4f;
    }

    /**
     * Get combined talent description for UI
     */
    public String getCombinedTalentDescription() {
        if (hasDivineDaoDestiny()) {
            return "Divine Dao Destiny - Heaven's Chosen One";
        } else if (hasLegendaryTalent()) {
            return "Legendary Talent - Once in a Generation";
        } else if (getSpiritualRootMultiplier() >= 1.3f || getPhysiqueMultiplier() >= 1.3f) {
            return "Rare Talent - Exceptional Potential";
        } else if (getSpiritualRootMultiplier() >= 1.2f || getPhysiqueMultiplier() >= 1.2f) {
            return "Uncommon Talent - Above Average";
        } else if (getSpiritualRootMultiplier() >= 1.1f || getPhysiqueMultiplier() >= 1.1f) {
            return "Common Talent - Decent Foundation";
        } else {
            return "Mortal Talent - Ordinary Aptitude";
        }
    }

    /**
     * Get overall cultivation multiplier (multiplicative combination)
     */
    public float getOverallCultivationMultiplier() {
        return spiritualRootType.getCultivationMultiplier() * physiqueType.getCultivationMultiplier();
    }

    // === UTILITY METHODS ===

    /**
     * Reset both talents (for debug/admin commands)
     */
    public void resetTalents() {
        ImmortalCultivation.LOGGER.warn("Resetting talents for player: {}",
            player != null ? player.getName().getString() : "Unknown");

        this.spiritualRootType = SpiritualRootType.MORTAL_ROOT;
        this.spiritualRootAssigned = false;
        this.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        this.physiqueAssigned = false;
        this.needsSync = true;
        forceSync();
    }

    /**
     * Force reassignment of talents (for special items/events)
     */
    public void forceReassignSpiritualRoot(SpiritualRootType type) {
        ImmortalCultivation.LOGGER.info("Force reassigning spiritual root for player {} from {} to {}",
            player != null ? player.getName().getString() : "Unknown",
            this.spiritualRootType.getDisplayName(),
            type.getDisplayName());

        this.spiritualRootType = type;
        this.spiritualRootAssigned = true;
        this.needsSync = true;
        forceSync();
    }

    public void forceReassignPhysique(PhysiqueType type) {
        ImmortalCultivation.LOGGER.info("Force reassigning physique for player {} from {} to {}",
            player != null ? player.getName().getString() : "Unknown",
            this.physiqueType.getDisplayName(),
            type.getDisplayName());

        this.physiqueType = type;
        this.physiqueAssigned = true;
        this.needsSync = true;
        forceSync();
    }

    // === LEGACY COMPATIBILITY METHODS ===

    /**
     * Check if talents have been assigned (unified check)
     */
    public boolean hasBeenAssigned() {
        return spiritualRootAssigned && physiqueAssigned;
    }

    /**
     * Assign spiritual root type (legacy compatibility)
     */
    public void assignRootType(SpiritualRootType generatedType) {
        assignSpiritualRoot(generatedType);
    }

    /**
     * Get spiritual root type (legacy compatibility)
     */
    public SpiritualRootType getRootType() {
        return spiritualRootType;
    }

    /**
     * Get cultivation multiplier (legacy compatibility - returns spiritual root only)
     */
    public float getCultivationMultiplier() {
        return getSpiritualRootMultiplier();
    }

    // === SYNC MANAGEMENT ===

    public void tick() {
        if (needsSync && player instanceof ServerPlayer) {
            syncToClient();
            needsSync = false;
        }
    }

    private void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                TalentSyncPacket packet = new TalentSyncPacket(
                    spiritualRootType.name(),
                    spiritualRootAssigned,
                    physiqueType.name(),
                    physiqueAssigned
                );
                PacketDistributor.sendToPlayer(serverPlayer, packet);

                ImmortalCultivation.LOGGER.debug("Synced talent data to client for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing talent data for player {}",
                    player.getName().getString(), e);
            }
        }
    }

    /**
     * CRITICAL: Force sync method for dimension changes and initialization.
     * This ensures the client receives updated data even if internal state hasn't changed.
     */
    public void forceSync() {
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                TalentSyncPacket packet = new TalentSyncPacket(
                    spiritualRootType.name(),
                    spiritualRootAssigned,
                    physiqueType.name(),
                    physiqueAssigned
                );
                PacketDistributor.sendToPlayer(serverPlayer, packet);

                ImmortalCultivation.LOGGER.debug("Force synced {} for player: {}",
                    this.getClass().getSimpleName(),
                    serverPlayer.getName().getString());

                needsSync = false;
            } else {
                ImmortalCultivation.LOGGER.debug("Cannot sync talent data - no player reference or not server player");
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing {} for player {}",
                this.getClass().getSimpleName(),
                player != null ? player.getName().getString() : "unknown", e);
        }
    }

    // === SERIALIZATION ===

    public static final Codec<PlayerTalentAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("spiritual_root_type").forGetter(d -> d.spiritualRootType.name()),
            Codec.BOOL.fieldOf("spiritual_root_assigned").forGetter(d -> d.spiritualRootAssigned),
            Codec.STRING.fieldOf("physique_type").forGetter(d -> d.physiqueType.name()),
            Codec.BOOL.fieldOf("physique_assigned").forGetter(d -> d.physiqueAssigned)
        ).apply(instance, PlayerTalentAttachment::decode)
    );

    private static PlayerTalentAttachment decode(String rootType, boolean rootAssigned,
                                                 String physiqueType, boolean physiqueAssigned) {
        PlayerTalentAttachment data = new PlayerTalentAttachment();

        try {
            data.spiritualRootType = SpiritualRootType.valueOf(rootType);
        } catch (IllegalArgumentException e) {
            ImmortalCultivation.LOGGER.warn("Invalid spiritual root type '{}', defaulting to MORTAL_ROOT", rootType);
            data.spiritualRootType = SpiritualRootType.MORTAL_ROOT;
        }
        data.spiritualRootAssigned = rootAssigned;

        try {
            data.physiqueType = PhysiqueType.valueOf(physiqueType);
        } catch (IllegalArgumentException e) {
            ImmortalCultivation.LOGGER.warn("Invalid physique type '{}', defaulting to MORTAL_PHYSIQUE", physiqueType);
            data.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        }
        data.physiqueAssigned = physiqueAssigned;

        return data;
    }

    // === SERIALIZER ===

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerTalentAttachment> {
        @Override
        public PlayerTalentAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerTalentAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerTalentAttachment: {}", error))
                .map(pair -> pair.getFirst())
                .orElse(new PlayerTalentAttachment());

            // CRITICAL: Set player reference during deserialization
            if (holder instanceof Player player) {
                attachment.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Set player reference during talent attachment deserialization: {}",
                    player.getName().getString());
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerTalentAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encode(attachment, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerTalentAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }

    // === DEBUG METHODS ===

    public void debugInfo() {
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Talent Data Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Spiritual Root: {} ({}x) - Assigned: {}",
            spiritualRootType.getDisplayName(),
            spiritualRootType.getCultivationMultiplier(),
            spiritualRootAssigned);
        ImmortalCultivation.LOGGER.info("Physique: {} ({}x) - Assigned: {}",
            physiqueType.getDisplayName(),
            physiqueType.getCultivationMultiplier(),
            physiqueAssigned);
        ImmortalCultivation.LOGGER.info("Overall Multiplier: {}x", getOverallCultivationMultiplier());
        ImmortalCultivation.LOGGER.info("Talent Level: {}", getCombinedTalentDescription());
        ImmortalCultivation.LOGGER.info("Divine Dao Destiny: {}", hasDivineDaoDestiny());
        ImmortalCultivation.LOGGER.info("===============================");
    }

    /**
     * Get a concise summary for command output
     */
    public String getSummary() {
        return String.format("Spiritual Root: %s (%.1fx) | Physique: %s (%.1fx) | Overall: %.2fx | %s",
            spiritualRootType.getDisplayName(),
            spiritualRootType.getCultivationMultiplier(),
            physiqueType.getDisplayName(),
            physiqueType.getCultivationMultiplier(),
            getOverallCultivationMultiplier(),
            getCombinedTalentDescription());
    }
}