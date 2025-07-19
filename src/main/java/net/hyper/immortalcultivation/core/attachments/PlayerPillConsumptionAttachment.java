package net.hyper.immortalcultivation.core.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.content.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerPillConsumptionSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Player attachment for tracking pill consumption and calculating diminishing returns.
 * Manages consumption counts for different pill types and rarities, implementing
 * effectiveness decay based on consumption history to prevent pill abuse.
 *
 * Extends PlayerAttachment to inherit debugging, synchronization, and performance monitoring capabilities.
 * Integrates pill effectiveness calculations directly into the attachment for better OOP design.
 */
public class PlayerPillConsumptionAttachment extends PlayerAttachment<PlayerPillConsumptionAttachment> {

    // === CONSTANTS === //

    private static final double MIN_EFFECTIVENESS = 0.1;
    private static final int PILLS_TO_MIN_EFFECTIVENESS = 100;
    private static final Map<PillFurnaceBlockEntity.PillRarity, Double> DECAY_FACTORS = Map.of(
        PillFurnaceBlockEntity.PillRarity.MORTAL, 2.3,
        PillFurnaceBlockEntity.PillRarity.PROFOUND, 2.0,
        PillFurnaceBlockEntity.PillRarity.DIVINE, 1.7,
        PillFurnaceBlockEntity.PillRarity.CELESTIAL, 1.4,
        PillFurnaceBlockEntity.PillRarity.HEAVENLY, 1.1
    );

    // === CONSUMPTION DATA FIELDS === //

    private final Map<String, Integer> consumptionData = new HashMap<>();

    // === CONSTRUCTORS === //

    /**
     * Default constructor for creating a new PlayerPillConsumptionAttachment with empty consumption data.
     */
    public PlayerPillConsumptionAttachment() {
        super();
    }

    // === CORE FUNCTIONALITY === //

    /**
     * Calculates the effectiveness of a pill based on previous consumption.
     * Uses exponential decay to reduce effectiveness as consumption increases.
     *
     * @param pillItem The pill item being consumed
     * @param rarity The rarity of the pill
     * @return Effectiveness value between MIN_EFFECTIVENESS and 1.0
     */
    public double calculateEffectiveness(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        if (debugEnabled()) {
            debugLog("Calculating effectiveness for pill: {} with rarity: {}",
                pillItem.toString(), rarity != null ? rarity.name() : "NULL");
        }

        String key = getPillKey(pillItem, rarity);
        int consumptionCount = consumptionData.getOrDefault(key, 0);
        return calculateEffectivenessAtCount(rarity, consumptionCount);
    }

    /**
     * Calculates effectiveness at a specific consumption count.
     * Static method for previewing future effectiveness without modifying state.
     *
     * @param rarity The pill rarity affecting decay rate
     * @param consumptionCount Number of times this pill type has been consumed
     * @return Effectiveness value between MIN_EFFECTIVENESS and 1.0
     */
    public static double calculateEffectivenessAtCount(PillFurnaceBlockEntity.PillRarity rarity, int consumptionCount) {
        if (consumptionCount == 0) {
            return 1.0;
        }

        double decayFactor = rarity != null ?
            DECAY_FACTORS.get(rarity) :
            DECAY_FACTORS.get(PillFurnaceBlockEntity.PillRarity.MORTAL);

        double effectiveness = MIN_EFFECTIVENESS +
            (1.0 - MIN_EFFECTIVENESS) *
                Math.exp(-decayFactor * consumptionCount / (double) PILLS_TO_MIN_EFFECTIVENESS);

        return Math.max(effectiveness, MIN_EFFECTIVENESS);
    }

    /**
     * Consumes a pill and returns its effectiveness, updating consumption tracking.
     * This method handles both effectiveness calculation and state management.
     *
     * @param pillItem The pill item being consumed
     * @param rarity The rarity of the pill
     * @return The effectiveness of this consumption (0.1 to 1.0)
     */
    public double consumePill(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        if (debugEnabled()) {
            debugLog("Consuming pill: {} with rarity: {}",
                pillItem.toString(), rarity != null ? rarity.name() : "NULL");
        }

        String key = getPillKey(pillItem, rarity);
        int currentCount = consumptionData.getOrDefault(key, 0);
        double effectiveness = calculateEffectiveness(pillItem, rarity);

        // Update consumption count and mark dirty
        consumptionData.put(key, currentCount + 1);
        onConsumptionChanged(key, currentCount, currentCount + 1);
        markDirty();

        if (debugEnabled()) {
            debugLog("Pill consumed - Count: {} -> {}, Effectiveness: {}",
                currentCount, currentCount + 1, effectiveness);
        }

        return effectiveness;
    }

    /**
     * Gets the number of times a specific pill type has been consumed.
     *
     * @param pillItem The pill item to check
     * @param rarity The rarity of the pill
     * @return Number of times consumed (0 if never consumed)
     */
    public int getConsumptionCount(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        return consumptionData.getOrDefault(key, 0);
    }

    /**
     * Previews the effectiveness of the next consumption without modifying state.
     * Useful for UI display and planning purposes.
     *
     * @param pillItem The pill item to preview
     * @param rarity The rarity of the pill
     * @return Expected effectiveness of the next consumption
     */
    public double getNextConsumptionEffectiveness(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        int currentCount = getConsumptionCount(pillItem, rarity);
        return calculateEffectivenessAtCount(rarity, currentCount);
    }

    /**
     * Called when a pill consumption count changes.
     * @param pillKey The pill key that changed
     * @param oldCount Previous consumption count
     * @param newCount New consumption count
     */
    protected void onConsumptionChanged(String pillKey, int oldCount, int newCount) {
        if (debugEnabled()) {
            debugLog("Consumption changed for {}: {} -> {}", pillKey, oldCount, newCount);
        }

        // Fire events or trigger additional logic here if needed
        lastStateChangeTime = System.currentTimeMillis();
    }

    /**
     * Generates a unique key for pill consumption tracking.
     * Combines item identifier with rarity for precise tracking.
     *
     * @param pillItem The pill item
     * @param rarity The pill rarity (null-safe)
     * @return Unique string key for consumption tracking
     */
    private String getPillKey(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String itemName = pillItem.toString();
        String rarityName = rarity != null ? rarity.name() : "NONE";
        return itemName + "_" + rarityName;
    }

    // === PLAYERATTACHMENT IMPLEMENTATION === //

    /**
     * Called when a player is set on this attachment.
     * Initializes any player-specific behavior and ensures data consistency.
     *
     * @param player The player that was set
     */
    @Override
    protected void onPlayerSet(Player player) {
        if (debugEnabled()) {
            debugLog("PlayerPillConsumptionAttachment associated with player: {}",
                player != null ? player.getName().getString() : "NULL");
        }

        // Validate consumption data integrity when player is set
        if (player != null && !consumptionData.isEmpty()) {
            int totalConsumptions = consumptionData.values().stream().mapToInt(Integer::intValue).sum();
            if (debugEnabled()) {
                debugLog("Player {} has {} total pill consumptions tracked",
                    player.getName().getString(), totalConsumptions);
            }
        }
    }

    /**
     * Gets the codec for serialization of this specific attachment type.
     *
     * @return The codec for PlayerPillConsumptionAttachment
     */
    @Override
    protected Codec<PlayerPillConsumptionAttachment> getCodec() {
        return CODEC;
    }

    /**
     * Creates a sync packet with the current attachment data.
     *
     * @param data The NBT data to include in the packet
     * @return The appropriate sync packet for pill consumption data
     */
    @Override
    protected ISyncPacket createSyncPacket(CompoundTag data) {
        return new PlayerPillConsumptionSyncPacket(data);
    }

    /**
     * Applies synchronization data from a sync packet.
     * Updates the consumption data and clears dirty state.
     *
     * @param data The NBT data from the sync packet
     */
    public void applySync(CompoundTag data) {
        startPerformanceTracking("applySync");
        try {
            deserialize(data);
            clearDirty(); // Important: mark as clean after sync
            onSyncPacketApplied(data, player);
        } finally {
            endPerformanceTracking("applySync", 3); // 3ms warning threshold
        }
    }

    // === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment for state comparison and sync management.
     *
     * @return A new PlayerPillConsumptionAttachment instance with identical data
     */
    @Override
    public PlayerPillConsumptionAttachment copy() {
        PlayerPillConsumptionAttachment copy = new PlayerPillConsumptionAttachment();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies all data from another PlayerPillConsumptionAttachment instance.
     *
     * @param other The attachment to copy data from
     */
    @Override
    protected void copyFrom(PlayerPillConsumptionAttachment other) {
        // Clear existing data and copy consumption data
        this.consumptionData.clear();
        this.consumptionData.putAll(other.consumptionData);

        if (debugEnabled()) {
            debugLog("Copied {} consumption entries from other attachment",
                other.consumptionData.size());
        }
    }

    /**
     * Checks if this attachment is equal to another instance for change detection.
     * Used by the sync system to determine if updates are needed.
     *
     * @param other The attachment to compare with
     * @return True if they contain identical data
     */
    @Override
    protected boolean isEqualTo(PlayerPillConsumptionAttachment other) {
        if (other == null) return false;

        // Compare consumption data maps
        boolean equal = this.consumptionData.equals(other.consumptionData);

        if (debugEnabled() && !equal) {
            debugLog("Attachment inequality detected - consumption data differs");
        }

        return equal;
    }

    // === SERIALIZATION SUPPORT === //

    /**
     * Serializes the attachment data to NBT format.
     *
     * @return CompoundTag containing all consumption data
     */
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        CompoundTag consumptionTag = new CompoundTag();

        for (Map.Entry<String, Integer> entry : consumptionData.entrySet()) {
            consumptionTag.putInt(entry.getKey(), entry.getValue());
        }

        tag.put("PillConsumption", consumptionTag);

        if (debugEnabled()) {
            debugLog("Serialized {} consumption entries to NBT", consumptionData.size());
        }

        return tag;
    }

    /**
     * Deserializes the attachment data from NBT format.
     *
     * @param tag CompoundTag containing consumption data
     */
    public void deserialize(CompoundTag tag) {
        consumptionData.clear();

        if (tag.contains("PillConsumption")) {
            CompoundTag consumptionTag = tag.getCompound("PillConsumption");
            for (String key : consumptionTag.getAllKeys()) {
                consumptionData.put(key, consumptionTag.getInt(key));
            }
        }

        if (debugEnabled()) {
            debugLog("Deserialized {} consumption entries from NBT", consumptionData.size());
        }

        markDirty(); // Mark dirty after deserialization to ensure sync
    }

    // === DEBUG SUPPORT === //

    /**
     * Adds specific state debug information for PlayerPillConsumptionAttachment.
     * Provides detailed consumption tracking information for debugging purposes.
     *
     * @param info The map to add debug information to
     */
    @Override
    protected void addSpecificStateDebugInfo(Map<String, Object> info) {
        // General consumption statistics
        info.put("totalPillTypes", consumptionData.size());
        info.put("totalConsumptions", consumptionData.values().stream().mapToInt(Integer::intValue).sum());

        // Most consumed pill information
        if (!consumptionData.isEmpty()) {
            Map.Entry<String, Integer> mostConsumed = consumptionData.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

            if (mostConsumed != null) {
                info.put("mostConsumedPill", mostConsumed.getKey());
                info.put("mostConsumedCount", mostConsumed.getValue());
            }
        }

        // Effectiveness ranges
        if (!consumptionData.isEmpty()) {
            double minEffectiveness = 1.0;
            double maxEffectiveness = 0.0;

            for (Map.Entry<String, Integer> entry : consumptionData.entrySet()) {
                // Parse rarity from key (simplified for debug purposes)
                PillFurnaceBlockEntity.PillRarity rarity = parseRarityFromKey(entry.getKey());
                double effectiveness = calculateEffectivenessAtCount(rarity, entry.getValue());
                minEffectiveness = Math.min(minEffectiveness, effectiveness);
                maxEffectiveness = Math.max(maxEffectiveness, effectiveness);
            }

            info.put("effectivenessRange", String.format("%.2f - %.2f", minEffectiveness, maxEffectiveness));
        }
    }

    /**
     * Override this method to add type-specific state validation.
     * @param issues The map to add validation issues to
     */
    @Override
    protected void addSpecificStateValidation(Map<String, String> issues) {
        // Validate consumption data
        for (Map.Entry<String, Integer> entry : consumptionData.entrySet()) {
            String key = entry.getKey();
            Integer count = entry.getValue();

            if (key == null || key.isEmpty()) {
                issues.put("invalidKey", "Found null or empty consumption key");
            }

            if (count == null || count < 0) {
                issues.put("invalidCount_" + key, "Invalid consumption count: " + count);
            }

            if (count != null && count > 10000) { // Sanity check
                issues.put("suspiciousCount_" + key, "Suspiciously high consumption count: " + count);
            }

            if (key != null && !key.contains("_")) {
                issues.put("malformedKey_" + key, "Key doesn't follow expected format: " + key);
            }
        }
    }

    /**
     * Gets a hash of the current state for comparison.
     * Used for efficient change detection in sync operations.
     *
     * @return State hash as a string
     */
    @Override
    public String getStateHash() {
        // Calculate a more detailed hash including consumption counts and effectiveness
        long hash = 0L;
        for (Map.Entry<String, Integer> entry : consumptionData.entrySet()) {
            hash = hash * 31L + entry.getKey().hashCode();
            hash = hash * 31L + entry.getValue().hashCode();
        }
        return String.valueOf(hash);
    }

    /**
     * Helper method to parse rarity from consumption key for debug purposes.
     *
     * @param key The consumption key
     * @return The parsed rarity, or MORTAL as fallback
     */
    private PillFurnaceBlockEntity.PillRarity parseRarityFromKey(String key) {
        if (key == null || !key.contains("_")) {
            return PillFurnaceBlockEntity.PillRarity.MORTAL;
        }

        String rarityPart = key.substring(key.lastIndexOf("_") + 1);
        try {
            return PillFurnaceBlockEntity.PillRarity.valueOf(rarityPart);
        } catch (IllegalArgumentException e) {
            return PillFurnaceBlockEntity.PillRarity.MORTAL;
        }
    }

    /**
     * Performs maintenance operations on consumption data.
     * Can be used to clean up invalid entries or optimize storage.
     */
    public void performMaintenance() {
        if (debugEnabled()) {
            debugLog("Performing maintenance on consumption data");
        }

        // Remove any invalid entries
        consumptionData.entrySet().removeIf(entry ->
            entry.getKey() == null || entry.getKey().isEmpty() ||
                entry.getValue() == null || entry.getValue() < 0
        );

        markDirty();
    }

    // === CODEC === //

    /**
     * Codec for serializing and deserializing PlayerPillConsumptionAttachment.
     * Uses a simple structure that directly encodes the consumption data map.
     */
    public static final Codec<PlayerPillConsumptionAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                .optionalFieldOf("consumption_data", new HashMap<>())
                .forGetter(attachment -> attachment.consumptionData)
        ).apply(instance, (consumptionData) -> {
            PlayerPillConsumptionAttachment attachment = new PlayerPillConsumptionAttachment();
            attachment.consumptionData.clear();
            attachment.consumptionData.putAll(consumptionData);
            return attachment;
        })
    );

    // === SERIALIZER === //

    /**
     * Serializer for PlayerPillConsumptionAttachment that handles NBT read/write operations.
     * Integrates with the NeoForge attachment system and provides debug support.
     */
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerPillConsumptionAttachment> {

        /**
         * Reads a PlayerPillConsumptionAttachment from NBT data.
         *
         * @param holder The attachment holder (typically a Player)
         * @param tag The NBT data to read from
         * @param provider The registry provider for context
         * @return The deserialized PlayerPillConsumptionAttachment
         */
        @Override
        public PlayerPillConsumptionAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerPillConsumptionAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerPillConsumptionAttachment: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerPillConsumptionAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            if (attachment.debugEnabled()) {
                attachment.debugLog("Successfully read attachment from NBT - {} consumption entries",
                    attachment.consumptionData.size());
            }

            return attachment;
        }

        /**
         * Writes a PlayerPillConsumptionAttachment to NBT data.
         *
         * @param attachment The attachment to serialize
         * @param provider The registry provider for context
         * @return The NBT data, or null if serialization fails
         */
        @Override
        public @Nullable CompoundTag write(PlayerPillConsumptionAttachment attachment, HolderLookup.Provider provider) {
            CompoundTag result = (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerPillConsumptionAttachment: {}", error))
                .orElse(new CompoundTag());

            if (attachment.debugEnabled()) {
                attachment.debugLog("Successfully wrote attachment to NBT - {} bytes",
                    result.toString().length());
            }

            return result;
        }
    }
}
