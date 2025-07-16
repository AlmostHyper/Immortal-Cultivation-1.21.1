package net.hyper.immortalcultivation.content.item.crops;

import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class MetalCropItems {

    public static Supplier<Item> WHITE_METAL_ROOTS() {
        return () -> new ICModCropItem(
            BlockRegistry.WHITE_METAL_ROOTS_CROP_BLOCK,
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}
