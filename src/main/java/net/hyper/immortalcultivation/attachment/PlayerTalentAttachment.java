package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.network.server.TalentSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Unified talent data attachment.
 * Merges: PlayerSpiritualRootAttachment and PlayerPhysiqueAttachment
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

    // === Player Reference Management ===

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Nullable
    protected Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === Spiritual Root Methods ===

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

    // === Physique Methods ===

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

    // === Combined Talent Methods ===

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

    // === Utility Methods ===

    /**
     * Reset both talents (for debug/admin commands)
     */
    public void resetTalents() {
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
        this.spiritualRootType = type;
        this.spiritualRootAssigned = true;
        this.needsSync = true;
        forceSync();
    }

    public void forceReassignPhysique(PhysiqueType type) {
        this.physiqueType = type;
        this.physiqueAssigned = true;
        this.needsSync = true;
        forceSync();
    }

    // === FIXED: Legacy compatibility methods ===

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
     * Get cultivation multiplier (legacy compatibility)
     */
    public float getCultivationMultiplier() {
        return getSpiritualRootMultiplier();
    }

    // === Sync Management ===

    public void tick() {
        if (needsSync && player instanceof ServerPlayer) {
            syncToClient();
            needsSync = false;
        }
    }

    private void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            TalentSyncPacket packet = new TalentSyncPacket(
                spiritualRootType.name(),
                spiritualRootAssigned,
                physiqueType.name(),
                physiqueAssigned
            );
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    public void forceSync() {
        if (player instanceof ServerPlayer) {
            syncToClient();
            needsSync = false;
        }
    }

    // === Serialization ===

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
            data.spiritualRootType = SpiritualRootType.MORTAL_ROOT;
        }
        data.spiritualRootAssigned = rootAssigned;

        try {
            data.physiqueType = PhysiqueType.valueOf(physiqueType);
        } catch (IllegalArgumentException e) {
            data.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        }
        data.physiqueAssigned = physiqueAssigned;

        return data;
    }

    // === Debug Methods ===

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
    }
}