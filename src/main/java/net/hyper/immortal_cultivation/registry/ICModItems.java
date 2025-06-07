package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.item.realm_1.spirit_stones.SpiritStone1LQItem;
import net.hyper.immortal_cultivation.item.realm_1.spirit_stones.SpiritStone2MQItem;
import net.hyper.immortal_cultivation.item.realm_1.spirit_stones.SpiritStone3HQItem;
import net.hyper.immortal_cultivation.item.realm_1.spirit_stones.SpiritStone4TQItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems("immortal_cultivation");

    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE;

    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;

    private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem((Block)block.get(), new Item.Properties()));
    }

    static {
        LOW_QUALITY_SPIRIT_STONE = REGISTRY.register("low_quality_spirit_stone", SpiritStone1LQItem::new);
        MEDIUM_QUALITY_SPIRIT_STONE = REGISTRY.register("medium_quality_spirit_stone", SpiritStone2MQItem::new);
        HIGH_QUALITY_SPIRIT_STONE = REGISTRY.register("high_quality_spirit_stone", SpiritStone3HQItem::new);
        TOP_QUALITY_SPIRIT_STONE = REGISTRY.register("top_quality_spirit_stone", SpiritStone4TQItem::new);

        LOW_QUALITY_SPIRIT_STONE_BLOCK = block(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK);
        LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK = block(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK);
        LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = block(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        MEDIUM_QUALITY_SPIRIT_STONE_BLOCK = block(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK);
        MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK = block(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK);
        MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = block(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        HIGH_QUALITY_SPIRIT_STONE_BLOCK = block(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK);
        HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = block(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        TOP_QUALITY_SPIRIT_STONE_BLOCK = block(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK);
        TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = block(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);

    }

}

