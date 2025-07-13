package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.IPlayerInitializable;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerTalentsSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Player talents attachment managing spiritual roots and physiques in the cultivation system.
 * Extends PlayerAttachment to inherit debugging, synchronization, and performance monitoring capabilities.
 * Implements IPlayerInitializable for automatic talent assignment on first login.
 */
public class PlayerTalentsAttachment extends PlayerAttachment<PlayerTalentsAttachment> implements IPlayerInitializable {

    // === TALENT FIELDS === //

    // --- Spiritual Root Data ---
    private SpiritualRootType spiritualRootType = SpiritualRootType.MORTAL_ROOT;
    private boolean spiritualRootAssigned = false;

    // --- Physique Data ---
    private PhysiqueType physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
    private boolean physiqueAssigned = false;

    // === CONSTRUCTORS === //

    /**
     * Default constructor for creating a new PlayerTalentsAttachment with default mortal talents.
     */
    public PlayerTalentsAttachment() {
        super();
    }

    /**
     * Constructor with player reference for immediate association.
     * @param player The player to associate with this attachment
     */
    public PlayerTalentsAttachment(Player player) {
        super();
        setPlayer(player);
    }

    // === TALENT TYPE ENUM === //

    /**
     * Enumeration of talent types managed by this attachment.
     */
    public enum TalentType {
        SPIRITUAL_ROOT("Spiritual Root"),
        PHYSIQUE("Physique");

        private final String displayName;

        TalentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === GENERIC TALENT MANAGEMENT === //

    /**
     * Gets the talent of the specified type.
     * @param type The talent type to retrieve
     * @return The talent object (SpiritualRootType or PhysiqueType)
     */
    public Object getTalent(TalentType type) {
        return switch (type) {
            case SPIRITUAL_ROOT -> spiritualRootType;
            case PHYSIQUE -> physiqueType;
        };
    }

    /**
     * Checks if the specified talent type has been assigned.
     * @param type The talent type to check
     * @return True if the talent is assigned
     */
    public boolean isTalentAssigned(TalentType type) {
        return switch (type) {
            case SPIRITUAL_ROOT -> spiritualRootAssigned;
            case PHYSIQUE -> physiqueAssigned;
        };
    }

    /**
     * Assigns a talent of the specified type if not already assigned.
     * @param type The talent type to assign
     * @param talent The talent object to assign
     * @return True if assignment was successful, false if already assigned or invalid type
     */
    public boolean assignTalent(TalentType type, Object talent) {
        if (isTalentAssigned(type)) {
            if (debugEnabled()) {
                debugLog("Attempted to reassign {} from {} to {} for player {}",
                    type.getDisplayName(), getTalent(type), talent,
                    player != null ? player.getName().getString() : "NULL");
            }
            return false;
        }

        boolean success = switch (type) {
            case SPIRITUAL_ROOT -> {
                if (talent instanceof SpiritualRootType rootType) {
                    this.spiritualRootType = rootType;
                    this.spiritualRootAssigned = true;
                    yield true;
                }
                yield false;
            }
            case PHYSIQUE -> {
                if (talent instanceof PhysiqueType physiqueType) {
                    this.physiqueType = physiqueType;
                    this.physiqueAssigned = true;
                    yield true;
                }
                yield false;
            }
        };

        if (success) {
            markDirty();
            if (debugEnabled()) {
                debugLog("Assigned {} {} to player {}",
                    type.getDisplayName(), talent,
                    player != null ? player.getName().getString() : "NULL");
            }
        } else if (debugEnabled()) {
            debugLog("Failed to assign {} - invalid type: {}", type.getDisplayName(),
                talent != null ? talent.getClass().getSimpleName() : "NULL");
        }

        return success;
    }

    /**
     * Gets the cultivation multiplier for the specified talent type.
     * @param type The talent type
     * @return Cultivation multiplier (1.0f = normal, higher = better)
     */
    public float getTalentMultiplier(TalentType type) {
        return switch (type) {
            case SPIRITUAL_ROOT -> spiritualRootType.getCultivationMultiplier();
            case PHYSIQUE -> physiqueType.getCultivationMultiplier();
        };
    }

    /**
     * Gets the combined cultivation multiplier from all assigned talents.
     * @return Combined multiplier for total cultivation effectiveness
     */
    public float getTotalCultivationMultiplier() {
        float multiplier = 1.0f;
        for (TalentType type : TalentType.values()) {
            multiplier *= getTalentMultiplier(type);
        }
        return multiplier;
    }

    /**
     * Checks if all talent types have been assigned.
     * @return True if all talents are assigned
     */
    public boolean hasAllTalentsAssigned() {
        for (TalentType type : TalentType.values()) {
            if (!isTalentAssigned(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the display name for the specified talent type.
     * @param type The talent type
     * @return The display name of the assigned talent
     */
    public String getTalentDisplayName(TalentType type) {
        return switch (type) {
            case SPIRITUAL_ROOT -> spiritualRootType.getDisplayName();
            case PHYSIQUE -> physiqueType.getDisplayName();
        };
    }

    // === CONVENIENCE METHODS === //

    /**
     * Gets the spiritual root type.
     * @return The assigned spiritual root type
     */
    public SpiritualRootType getSpiritualRootType() {
        return spiritualRootType;
    }

    /**
     * Gets the physique type.
     * @return The assigned physique type
     */
    public PhysiqueType getPhysiqueType() {
        return physiqueType;
    }

    /**
     * Assigns a spiritual root.
     * @param type The spiritual root type to assign
     * @return True if assignment was successful
     */
    public boolean assignSpiritualRoot(SpiritualRootType type) {
        return assignTalent(TalentType.SPIRITUAL_ROOT, type);
    }

    /**
     * Assigns a physique.
     * @param type The physique type to assign
     * @return True if assignment was successful
     */
    public boolean assignPhysique(PhysiqueType type) {
        return assignTalent(TalentType.PHYSIQUE, type);
    }

    // === PLAYERATTACHMENT IMPLEMENTATIONS === //

    /**
     * Initializes any player-specific behavior.
     * @param player The player that was set
     */
    @Override
    protected void onPlayerSet(Player player) {
        if (debugEnabled()) {
            debugLog("PlayerTalentsAttachment associated with player: {}",
                player != null ? player.getName().getString() : "NULL");
        }

        // No recalculation needed for talents as they are static once assigned
    }

    /**
     * Gets the codec for serialization of this specific attachment type.
     * @return The codec for PlayerTalentsAttachment
     */
    @Override
    protected Codec<PlayerTalentsAttachment> getCodec() {
        return CODEC;
    }

    /**
     * Creates a sync packet with the current attachment data.
     * @param data The NBT data to include in the packet
     * @return The appropriate sync packet
     */
    @Override
    protected ISyncPacket createSyncPacket(CompoundTag data) {
        return new PlayerTalentsSyncPacket(data);
    }

    // === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment for state comparison and sync management.
     * @return A new PlayerTalentsAttachment instance with identical data
     */
    @Override
    public PlayerTalentsAttachment copy() {
        PlayerTalentsAttachment copy = new PlayerTalentsAttachment();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies all data from another PlayerTalentsAttachment instance.
     * @param other The attachment to copy data from
     */
    @Override
    protected void copyFrom(PlayerTalentsAttachment other) {
        // Talent data
        this.spiritualRootType = other.spiritualRootType;
        this.spiritualRootAssigned = other.spiritualRootAssigned;
        this.physiqueType = other.physiqueType;
        this.physiqueAssigned = other.physiqueAssigned;
    }

    /**
     * Checks if this attachment is equal to another instance for change detection.
     * @param other The attachment to compare with
     * @return True if they contain the same data
     */
    @Override
    protected boolean isEqualTo(PlayerTalentsAttachment other) {
        if (other == null) return false;

        return this.spiritualRootType == other.spiritualRootType &&
            this.spiritualRootAssigned == other.spiritualRootAssigned &&
            this.physiqueType == other.physiqueType &&
            this.physiqueAssigned == other.physiqueAssigned;
    }

    // === IPLAYER_INITIALIZABLE IMPLEMENTATION === //

    /**
     * Called when a player logs in to assign talents if they haven't been assigned yet.
     * This ensures all players have both spiritual root and physique assigned.
     * @param player The server player logging in
     */
    @Override
    public void onPlayerLogin(ServerPlayer player) {
        boolean needsSync = false;
        RandomSource random = player.getRandom();

        // Assign spiritual root if not already assigned
        if (!isTalentAssigned(TalentType.SPIRITUAL_ROOT)) {
            SpiritualRootType newRoot = SpiritualRootType.generateRandom(random);
            if (assignSpiritualRoot(newRoot)) {
                player.sendSystemMessage(Component.literal("You have awakened the: " + newRoot.getDisplayName())
                    .withStyle(ChatFormatting.AQUA));

                if (debugEnabled()) {
                    debugLog("Assigned new spiritual root {} to player {}",
                        newRoot.getDisplayName(), player.getName().getString());
                }
                needsSync = true;
            }
        }

        // Assign physique if not already assigned
        if (!isTalentAssigned(TalentType.PHYSIQUE)) {
            PhysiqueType newPhysique = PhysiqueType.generateRandom(random);
            if (assignPhysique(newPhysique)) {
                player.sendSystemMessage(Component.literal("You possess the: " + newPhysique.getDisplayName())
                    .withStyle(ChatFormatting.RED));

                if (debugEnabled()) {
                    debugLog("Assigned new physique {} to player {}",
                        newPhysique.getDisplayName(), player.getName().getString());
                }
                needsSync = true;
            }
        }

        // Log existing talents for debug purposes
        if (debugEnabled() && !needsSync) {
            debugLog("Player {} logged in with Spiritual Root: {} and Physique: {}",
                player.getName().getString(),
                getSpiritualRootType().getDisplayName(),
                getPhysiqueType().getDisplayName());
        }

        // Force sync if new talents were assigned
        if (needsSync) {
            markDirty();
        }
    }

    // === CODEC IMPLEMENTATION === //

    /**
     * Sub-codec for spiritual root data
     */
    private static final Codec<PlayerTalentsAttachment> SPIRITUAL_ROOT_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("spiritual_root_type", SpiritualRootType.MORTAL_ROOT.name())
                .forGetter(d -> d.spiritualRootType.name()),
            Codec.BOOL.optionalFieldOf("spiritual_root_assigned", false)
                .forGetter(d -> d.spiritualRootAssigned)
        ).apply(instance, (rootTypeName, rootAssigned) -> {
            PlayerTalentsAttachment data = new PlayerTalentsAttachment();

            // Parse spiritual root type with fallback
            try {
                data.spiritualRootType = SpiritualRootType.valueOf(rootTypeName);
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Invalid spiritual root type '{}', defaulting to MORTAL_ROOT", rootTypeName);
                data.spiritualRootType = SpiritualRootType.MORTAL_ROOT;
            }
            data.spiritualRootAssigned = rootAssigned;

            return data;
        })
    );

    /**
     * Sub-codec for physique data
     */
    private static final Codec<PlayerTalentsAttachment> PHYSIQUE_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("physique_type", PhysiqueType.MORTAL_PHYSIQUE.name())
                .forGetter(d -> d.physiqueType.name()),
            Codec.BOOL.optionalFieldOf("physique_assigned", false)
                .forGetter(d -> d.physiqueAssigned)
        ).apply(instance, (physiqueTypeName, physiqueAssigned) -> {
            PlayerTalentsAttachment data = new PlayerTalentsAttachment();

            // Parse physique type with fallback
            try {
                data.physiqueType = PhysiqueType.valueOf(physiqueTypeName);
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Invalid physique type '{}', defaulting to MORTAL_PHYSIQUE", physiqueTypeName);
                data.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
            }
            data.physiqueAssigned = physiqueAssigned;

            return data;
        })
    );

    /**
     * Main codec for PlayerTalentsAttachment that combines all sub-codecs.
     * Handles all talent data with proper error recovery and validation.
     */
    public static final Codec<PlayerTalentsAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            SPIRITUAL_ROOT_CODEC.fieldOf("spiritual_root_data").forGetter(d -> d),
            PHYSIQUE_CODEC.fieldOf("physique_data").forGetter(d -> d)
        ).apply(instance, (spiritualRootData, physiqueData) -> {
            PlayerTalentsAttachment data = new PlayerTalentsAttachment();

            // Copy spiritual root data
            data.spiritualRootType = spiritualRootData.spiritualRootType;
            data.spiritualRootAssigned = spiritualRootData.spiritualRootAssigned;

            // Copy physique data
            data.physiqueType = physiqueData.physiqueType;
            data.physiqueAssigned = physiqueData.physiqueAssigned;

            return data;
        })
    );

    // === SERIALIZER === //

    /**
     * Serializer for PlayerTalentsAttachment that handles NBT read/write operations.
     * Integrates with the NeoForge attachment system and provides debug support.
     */
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerTalentsAttachment> {

        /**
         * Reads a PlayerTalentsAttachment from NBT data.
         * @param holder The attachment holder (typically a Player)
         * @param tag The NBT data to read from
         * @param provider The registry provider for context
         * @return The deserialized PlayerTalentsAttachment
         */
        @Override
        public PlayerTalentsAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerTalentsAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerTalentsAttachment: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerTalentsAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            if (attachment.debugEnabled()) {
                attachment.debugLog("Successfully read PlayerTalentsAttachment - Root: {}, Physique: {}",
                    attachment.getSpiritualRootType().getDisplayName(),
                    attachment.getPhysiqueType().getDisplayName());
            }

            return attachment;
        }

        /**
         * Writes a PlayerTalentsAttachment to NBT data.
         * @param attachment The attachment to serialize
         * @param provider The registry provider for context
         * @return The serialized NBT data
         */
        @Override
        public @Nullable CompoundTag write(PlayerTalentsAttachment attachment, HolderLookup.Provider provider) {
            if (attachment == null) {
                ImmortalCultivation.LOGGER.warn("Attempted to write null PlayerTalentsAttachment");
                return null;
            }

            try {
                CompoundTag result = (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerTalentsAttachment: {}", error))
                    .orElse(new CompoundTag());

                if (attachment.debugEnabled()) {
                    attachment.debugLog("Successfully wrote PlayerTalentsAttachment - Size: {} bytes",
                        result.toString().length());
                }

                return result;
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Exception while writing PlayerTalentsAttachment: {}", e.getMessage(), e);
                return new CompoundTag(); // Return empty tag as fallback
            }
        }
    }

    // === DEBUG SUPPORT === //

    /**
     * Adds specific state debug information for PlayerTalentsAttachment.
     * @param info The map to add debug information to
     */
    @Override
    protected void addSpecificStateDebugInfo(Map<String, Object> info) {
        // Talent assignment status
        for (TalentType type : TalentType.values()) {
            String key = type.name().toLowerCase() + "Assigned";
            info.put(key, isTalentAssigned(type));

            if (isTalentAssigned(type)) {
                String displayKey = type.name().toLowerCase() + "Display";
                String multiplierKey = type.name().toLowerCase() + "Multiplier";
                info.put(displayKey, getTalentDisplayName(type));
                info.put(multiplierKey, getTalentMultiplier(type));
            }
        }

        // Combined information
        info.put("totalMultiplier", getTotalCultivationMultiplier());
        info.put("allTalentsAssigned", hasAllTalentsAssigned());
    }

    /**
     * Validates the internal state and returns any issues found.
     * @return Map of validation issues (empty if no issues)
     */
    @Override
    public Map<String, String> validateState() {
        Map<String, String> issues = new HashMap<>();

        // Validate spiritual root
        if (spiritualRootType == null) {
            issues.put("spiritualRootType", "Null spiritual root type");
        }

        // Validate physique
        if (physiqueType == null) {
            issues.put("physiqueType", "Null physique type");
        }

        // Validate assignment consistency
        if (spiritualRootAssigned && spiritualRootType == SpiritualRootType.MORTAL_ROOT) {
            issues.put("spiritualRootConsistency", "Marked as assigned but still mortal root");
        }

        if (physiqueAssigned && physiqueType == PhysiqueType.MORTAL_PHYSIQUE) {
            issues.put("physiqueConsistency", "Marked as assigned but still mortal physique");
        }

        // Validate multipliers
        for (TalentType type : TalentType.values()) {
            float multiplier = getTalentMultiplier(type);
            if (multiplier <= 0) {
                issues.put(type.name().toLowerCase() + "Multiplier", "Non-positive multiplier: " + multiplier);
            }
        }

        return issues;
    }

    /**
     * Gets a hash of the current state for comparison.
     * @return State hash string for change detection
     */
    @Override
    public String getStateHash() {
        return String.format("%s:%b|%s:%b|%.2f",
            spiritualRootType.name(), spiritualRootAssigned,
            physiqueType.name(), physiqueAssigned,
            getTotalCultivationMultiplier());
    }
}
