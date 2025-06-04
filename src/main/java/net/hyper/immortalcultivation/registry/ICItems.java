package net.hyper.immortalcultivation.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("immortal_cultivation");

    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE = ITEMS.register("low_quality_spirit_stone", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE = ITEMS.register("medium_quality_spirit_stone", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE = ITEMS.register("high_quality_spirit_stone", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE = ITEMS.register("top_quality_spirit_stone", () -> new Item(new Item.Properties()));
}

