package net.hyper.immortalcultivation.item.crops;

import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class EarthCropItems {

    public static Supplier<Item> YELLOW_EARTH_ROOTS() {
        return () -> new ICModCropItem(
            ICModBlocks.YELLOW_EARTH_ROOTS_CROP_BLOCK,
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}
