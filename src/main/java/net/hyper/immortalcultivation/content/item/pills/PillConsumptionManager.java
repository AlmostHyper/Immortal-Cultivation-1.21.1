package net.hyper.immortalcultivation.content.item.pills;

import net.hyper.immortalcultivation.content.block.entities.PillFurnaceBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages pill consumption tracking and diminishing returns calculations.
 * Implements a sophisticated system where pill effectiveness diminishes based on consumption,
 * with higher rarities experiencing slower diminishing returns.
 */
public class PillConsumptionManager {

    // Consumption data: Map<PillType_Rarity, ConsumptionCount>
    private final Map<String, Integer> consumptionData = new HashMap<>();

    // Diminishing returns constants
    private static final double MIN_EFFECTIVENESS = 0.1; // 10% minimum effectiveness
    private static final int PILLS_TO_MIN_EFFECTIVENESS = 100; // Pills needed to reach minimum

    // Decay factors for different rarities (higher rarity = slower decay)
    private static final Map<PillFurnaceBlockEntity.PillRarity, Double> DECAY_FACTORS = Map.of(
        PillFurnaceBlockEntity.PillRarity.MORTAL, 2.3,    // Fastest decay
        PillFurnaceBlockEntity.PillRarity.PROFOUND, 2.0,
        PillFurnaceBlockEntity.PillRarity.DIVINE, 1.7,
        PillFurnaceBlockEntity.PillRarity.CELESTIAL, 1.4,
        PillFurnaceBlockEntity.PillRarity.HEAVENLY, 1.1   // Slowest decay
    );

    /**
     * Calculate the effectiveness multiplier for a pill based on consumption history.
     * Uses exponential decay formula to ensure smooth diminishing returns.
     *
     * @param pillItem The base pill item
     * @param rarity The pill's rarity (can be null for base pills)
     * @return Effectiveness multiplier between MIN_EFFECTIVENESS and 1.0
     */
    public double calculateEffectiveness(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        int consumptionCount = consumptionData.getOrDefault(key, 0);

        if (consumptionCount == 0) {
            return 1.0; // Full effectiveness for first consumption
        }

        // Get decay factor based on rarity (default to MORTAL if no rarity)
        double decayFactor = rarity != null ?
            DECAY_FACTORS.get(rarity) :
            DECAY_FACTORS.get(PillFurnaceBlockEntity.PillRarity.MORTAL);

        // Exponential decay formula: min + (1 - min) * exp(-decay * count / pills_to_min)
        double effectiveness = MIN_EFFECTIVENESS +
            (1.0 - MIN_EFFECTIVENESS) *
                Math.exp(-decayFactor * consumptionCount / (double) PILLS_TO_MIN_EFFECTIVENESS);

        // Ensure we never go below minimum effectiveness
        return Math.max(effectiveness, MIN_EFFECTIVENESS);
    }

    /**
     * Record the consumption of a pill and return the effectiveness for this consumption.
     * This should be called when a pill is actually consumed.
     *
     * @param pillItem The base pill item
     * @param rarity The pill's rarity (can be null for base pills)
     * @return The effectiveness multiplier for this consumption
     */
    public double consumePill(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        int currentCount = consumptionData.getOrDefault(key, 0);

        // Calculate effectiveness BEFORE incrementing (since this is the Nth consumption)
        double effectiveness = calculateEffectiveness(pillItem, rarity);

        // Increment consumption count
        consumptionData.put(key, currentCount + 1);

        return effectiveness;
    }

    /**
     * Get the current consumption count for a specific pill type and rarity.
     *
     * @param pillItem The base pill item
     * @param rarity The pill's rarity (can be null for base pills)
     * @return The number of times this pill has been consumed
     */
    public int getConsumptionCount(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        return consumptionData.getOrDefault(key, 0);
    }

    /**
     * Get the effectiveness for the next consumption without actually consuming.
     * Useful for tooltip displays.
     *
     * @param pillItem The base pill item
     * @param rarity The pill's rarity (can be null for base pills)
     * @return The effectiveness multiplier for the next consumption
     */
    public double getNextConsumptionEffectiveness(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        return calculateEffectiveness(pillItem, rarity);
    }

    /**
     * Generate a unique key for pill type and rarity combination.
     * Format: "itemRegistryName_rarityName" or "itemRegistryName_NONE" for no rarity
     */
    private String getPillKey(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String itemName = pillItem.toString(); // Gets registry name
        String rarityName = rarity != null ? rarity.name() : "NONE";
        return itemName + "_" + rarityName;
    }

    /**
     * Serialize consumption data to NBT for persistence.
     *
     * @return CompoundTag containing all consumption data
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag consumptionTag = new CompoundTag();

        for (Map.Entry<String, Integer> entry : consumptionData.entrySet()) {
            consumptionTag.putInt(entry.getKey(), entry.getValue());
        }

        tag.put("PillConsumption", consumptionTag);
        return tag;
    }

    /**
     * Deserialize consumption data from NBT.
     *
     * @param tag CompoundTag containing consumption data
     */
    public void deserializeNBT(CompoundTag tag) {
        consumptionData.clear();

        if (tag.contains("PillConsumption")) {
            CompoundTag consumptionTag = tag.getCompound("PillConsumption");
            for (String key : consumptionTag.getAllKeys()) {
                consumptionData.put(key, consumptionTag.getInt(key));
            }
        }
    }

    /**
     * Get all consumption data for debugging/admin purposes.
     *
     * @return Copy of the consumption data map
     */
    public Map<String, Integer> getAllConsumptionData() {
        return new HashMap<>(consumptionData);
    }

    /**
     * Clear consumption data for a specific pill (admin/debug use).
     *
     * @param pillItem The base pill item
     * @param rarity The pill's rarity (can be null for base pills)
     */
    public void clearConsumptionData(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        consumptionData.remove(key);
    }

    /**
     * Reset all consumption data (admin/debug use).
     */
    public void clearAllConsumptionData() {
        consumptionData.clear();
    }

    /**
     * Calculate what the effectiveness would be at a specific consumption count.
     * Useful for displaying diminishing returns curves or planning.
     *
     * @param rarity The pill's rarity
     * @param consumptionCount The hypothetical consumption count
     * @return The effectiveness multiplier at that consumption count
     */
    public static double calculateEffectivenessAtCount(PillFurnaceBlockEntity.PillRarity rarity, int consumptionCount) {
        if (consumptionCount <= 0) {
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
}