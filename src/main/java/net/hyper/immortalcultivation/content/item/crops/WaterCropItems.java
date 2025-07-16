package net.hyper.immortalcultivation.content.item.crops;

import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class WaterCropItems {

    public static Supplier<Item> BLACK_WATER_ROOTS() {
        return () -> new ICModCropItem(
            BlockRegistry.BLACK_WATER_ROOTS_CROP_BLOCK,
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}
