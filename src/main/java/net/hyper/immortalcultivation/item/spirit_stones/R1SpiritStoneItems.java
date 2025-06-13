package net.hyper.immortalcultivation.item.spirit_stones;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class R1SpiritStoneItems extends SpiritStoneItem{

    public static Supplier<Item> LOW_QUALITY_SPIRIT_STONE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .fireResistant()
            .rarity(Rarity.COMMON));
    }
    public static Supplier<Item> MEDIUM_QUALITY_SPIRIT_STONE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .fireResistant()
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> HIGH_QUALITY_SPIRIT_STONE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .fireResistant()
            .rarity(Rarity.RARE));
    }
    public static Supplier<Item> TOP_QUALITY_SPIRIT_STONE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .fireResistant()
            .rarity(Rarity.EPIC));
    }

    public R1SpiritStoneItems(Properties properties) {
        super(properties);
    }
}
