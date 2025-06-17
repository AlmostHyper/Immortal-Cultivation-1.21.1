package net.hyper.immortalcultivation.item.crops;

import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class MetalCropItems {

    public static Supplier<Item> WHITE_METAL_ROOTS() {
        return () -> new ICModCropItem(
            ICModBlocks.WHITE_METAL_ROOTS_CROP_BLOCK,
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}
