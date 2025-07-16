package net.hyper.immortalcultivation.content.item.crops;

import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class WoodCropItems {

    public static Supplier<Item> BLUE_WOOD_ROOTS() {
        return () -> new ICModCropItem(
            BlockRegistry.BLUE_WOOD_ROOTS_CROP_BLOCK,
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}
