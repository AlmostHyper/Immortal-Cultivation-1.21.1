package net.hyper.immortalcultivation.content.item.pills;

import net.hyper.immortalcultivation.content.block.entities.PillFurnaceBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class PillConsumptionManager {

    private final Map<String, Integer> consumptionData = new HashMap<>();
    private transient boolean isDirty = false;

    private static final double MIN_EFFECTIVENESS = 0.1;
    private static final int PILLS_TO_MIN_EFFECTIVENESS = 100;
    private static final Map<PillFurnaceBlockEntity.PillRarity, Double> DECAY_FACTORS = Map.of(
        PillFurnaceBlockEntity.PillRarity.MORTAL, 2.3,
        PillFurnaceBlockEntity.PillRarity.PROFOUND, 2.0,
        PillFurnaceBlockEntity.PillRarity.DIVINE, 1.7,
        PillFurnaceBlockEntity.PillRarity.CELESTIAL, 1.4,
        PillFurnaceBlockEntity.PillRarity.HEAVENLY, 1.1
    );

    public double calculateEffectiveness(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        int consumptionCount = consumptionData.getOrDefault(key, 0);

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

    public double consumePill(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String key = getPillKey(pillItem, rarity);
        int currentCount = consumptionData.getOrDefault(key, 0);
        double effectiveness = calculateEffectiveness(pillItem, rarity);
        consumptionData.put(key, currentCount + 1);
        markDirty();
        return effectiveness;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        this.isDirty = false;
    }

    private void markDirty() {
        this.isDirty = true;
    }
    
    private String getPillKey(Item pillItem, PillFurnaceBlockEntity.PillRarity rarity) {
        String itemName = pillItem.toString();
        String rarityName = rarity != null ? rarity.name() : "NONE";
        return itemName + "_" + rarityName;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag consumptionTag = new CompoundTag();

        for (Map.Entry<String, Integer> entry : consumptionData.entrySet()) {
            consumptionTag.putInt(entry.getKey(), entry.getValue());
        }

        tag.put("PillConsumption", consumptionTag);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        consumptionData.clear();
        if (tag.contains("PillConsumption")) {
            CompoundTag consumptionTag = tag.getCompound("PillConsumption");
            for (String key : consumptionTag.getAllKeys()) {
                consumptionData.put(key, consumptionTag.getInt(key));
            }
        }
        markDirty(); // Mark dirty after deserialization to ensure sync
    }
}
