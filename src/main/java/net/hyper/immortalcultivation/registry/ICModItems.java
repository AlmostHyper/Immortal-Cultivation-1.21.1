package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.item.realm1.SpiritStone1LQItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems("immortal_cultivation");

    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE;

    static {
        LOW_QUALITY_SPIRIT_STONE = REGISTRY.register("low_quality_spirit_stone", SpiritStone1LQItem::new);
    }

}

