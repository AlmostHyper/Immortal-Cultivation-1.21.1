package net.hyper.immortalcultivation.item.crops;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public class EarthCropItems {

    public static Supplier<Item> YELLOW_EARTH_ROOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.COMMON));
    }
}
