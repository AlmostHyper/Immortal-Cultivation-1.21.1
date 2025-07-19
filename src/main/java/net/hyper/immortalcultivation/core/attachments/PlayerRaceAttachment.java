package net.hyper.immortalcultivation.core.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.interfaces.IPlayerInitializable;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.NetworkRegistry;
import net.hyper.immortalcultivation.core.network.packets.server.OpenRaceSelectionSyncPacket;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerRaceSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;

/**
 * Player race attachment that manages race selection, assignment, and racial bonuses
 * in the cultivation system. Handles race-specific multipliers for Qi and Body cultivation
 * and ensures one-time race assignment with proper validation.
 * Extends PlayerAttachment to inherit debugging, synchronization, and performance monitoring capabilities.
 * Implements IPlayerInitializable for race selection UI presentation on first login.
 */
public class PlayerRaceAttachment extends PlayerAttachment<PlayerRaceAttachment> implements IPlayerInitializable {

    // === CONSTANTS === //

    private static final RaceType DEFAULT_RACE = RaceType.HUMAN;
    private static final boolean DEFAULT_RACE_ASSIGNED = false;

    // === RACE DATA FIELDS === //

    // --- Core Race Data ---
    private RaceType raceType = DEFAULT_RACE;
    private boolean raceAssigned = DEFAULT_RACE_ASSIGNED;

    // === CONSTRUCTORS === //

    /**
     * Default constructor for creating a new PlayerRaceAttachment with default human race.
     */
    public PlayerRaceAttachment() {
        super();
    }

    /**
     * Constructor with player reference for immediate association.
     * @param player The player to associate with this attachment
     */
    public PlayerRaceAttachment(@Nullable Player player) {
        super();
        setPlayer(player);
    }

    // === GETTERS & SETTERS === //

    /**
     * Gets the current race type of the player.
     * @return The player's race type
     */
    public RaceType getRaceType() {
        return raceType;
    }

    /**
     * Checks if the player has been assigned a race.
     * @return True if race has been assigned, false otherwise
     */
    public boolean hasRaceAssigned() {
        return raceAssigned;
    }

    /**
     * Gets the cultivation multiplier for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return The multiplier value for the specified cultivation type
     */
    public float getCultivationMultiplier(ICultivationData.CultivationType type) {
        return switch (type) {
            case QI -> raceType.getQiMultiplier();
            case BODY -> raceType.getBodyMultiplier();
        };
    }

    // === CORE RACE MANAGEMENT === //

    /**
     * Assigns a race to the player if no race has been assigned yet.
     * This is a one-time operation that cannot be undone.
     * @param type The race type to assign
     * @return True if assignment was successful, false if race was already assigned
     */
    public boolean assignRace(RaceType type) {
        if (raceAssigned) {
            if (debugEnabled()) {
                debugLog("Attempted to assign race {} but race already assigned: {}",
                    type.getDisplayName(), raceType.getDisplayName());
            }
            return false;
        }

        if (debugEnabled()) {
            debugLog("Assigning race: {} -> {}",
                raceType.getDisplayName(), type.getDisplayName());
        }

        this.raceType = type;
        this.raceAssigned = true;
        markDirty();

        if (debugEnabled()) {
            debugLog("Race assignment successful: {}", type.getDisplayName());
        }

        return true;
    }

    // === REQUIRED ABSTRACT METHOD IMPLEMENTATIONS === //

    /**
     * Initializes any player-specific behavior when a player is set.
     * @param player The player that was set
     */
    @Override
    protected void onPlayerSet(Player player) {
        if (debugEnabled()) {
            debugLog("PlayerRaceAttachment associated with player: {}",
                player != null ? player.getName().getString() : "NULL");
        }

        // No specific calculations needed for race attachment on player set
        if (debugEnabled() && player != null) {
            debugLog("Race attachment initialized for {}: Race={}, Assigned={}",
                player.getName().getString(), raceType.getDisplayName(), raceAssigned);
        }
    }

    /**
     * Gets the codec for serialization of this specific attachment type.
     * @return The codec for PlayerRaceAttachment
     */
    @Override
    protected Codec<PlayerRaceAttachment> getCodec() {
        return CODEC;
    }

    /**
     * Creates a sync packet with the current attachment data.
     * @param data The NBT data to include in the packet
     * @return The appropriate sync packet
     */
    @Override
    protected ISyncPacket createSyncPacket(CompoundTag data) {
        return new PlayerRaceSyncPacket(data);
    }

// === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment for state comparison and sync management.
     * @return A new PlayerRaceAttachment instance with identical data
     */
    @Override
    public PlayerRaceAttachment copy() {
        PlayerRaceAttachment copy = new PlayerRaceAttachment();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies all data from another PlayerRaceAttachment instance.
     * @param other The attachment to copy data from
     */
    @Override
    protected void copyFrom(PlayerRaceAttachment other) {
        this.raceType = other.raceType;
        this.raceAssigned = other.raceAssigned;
    }

    /**
     * Checks if this attachment is equal to another instance for change detection.
     * @param other The attachment to compare with
     * @return True if all data fields are equal
     */
    @Override
    protected boolean isEqualTo(PlayerRaceAttachment other) {
        return this.raceType == other.raceType &&
            this.raceAssigned == other.raceAssigned;
    }

    // === IPLAYERINITIALIZABLE IMPLEMENTATION === //

    /**
     * Called when a player logs into the server.
     * Sends race selection UI if no race has been assigned yet.
     * @param player The player who has just logged in
     */
    @Override
    public void onPlayerLogin(ServerPlayer player) {
        if (debugEnabled()) {
            debugLog("Processing login for player: {}, Race assigned: {}",
                player.getName().getString(), raceAssigned);
        }

        if (!hasRaceAssigned()) {
            // Send race selection screen to new player
            NetworkRegistry.sendToPlayer(new OpenRaceSelectionSyncPacket(), player);
            player.sendSystemMessage(Component.literal("Welcome, young cultivator! Choose your race to begin your journey.")
                .withStyle(ChatFormatting.GOLD));

            if (debugEnabled()) {
                debugLog("Sent race selection screen to new player {}", player.getName().getString());
            }
        } else {
            if (debugEnabled()) {
                debugLog("Player {} logged in with race: {}",
                    player.getName().getString(), getRaceType().getDisplayName());
            }
        }
    }

    // === SERIALIZATION === //

    /**
     * Codec for PlayerRaceAttachment serialization and deserialization.
     * Handles race type and assignment status with proper error handling.
     */
    public static final Codec<PlayerRaceAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("race_type").forGetter(d -> d.raceType.name()),
            Codec.BOOL.fieldOf("race_assigned").forGetter(d -> d.raceAssigned)
        ).apply(instance, (raceType, raceAssigned) -> {
            PlayerRaceAttachment data = new PlayerRaceAttachment();
            try {
                data.raceType = RaceType.valueOf(raceType);
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Invalid race type '{}' in save data, defaulting to HUMAN", raceType);
                data.raceType = DEFAULT_RACE;
            }
            data.raceAssigned = raceAssigned;
            return data;
        })
    );

// === SERIALIZER === //

    /**
     * Serializer for PlayerRaceAttachment that handles NBT read/write operations.
     * Integrates with the NeoForge attachment system and provides debug support.
     */
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerRaceAttachment> {

        /**
         * Reads a PlayerRaceAttachment from NBT data.
         * @param holder The attachment holder (typically a Player)
         * @param tag The NBT data to read from
         * @param provider The registry provider for context
         * @return The deserialized PlayerRaceAttachment
         */
        @Override
        public PlayerRaceAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerRaceAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerRaceAttachment: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerRaceAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        /**
         * Writes a PlayerRaceAttachment to NBT data.
         * @param attachment The attachment to serialize
         * @param provider The registry provider for context
         * @return The serialized NBT data
         */
        @Override
        public @Nullable CompoundTag write(PlayerRaceAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerRaceAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }
}
