package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.block.realm_1.spirit_stones.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ICModBlocks {
    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks("immortal_cultivation");

    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;


    static {
        LOW_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("low_quality_spirit_stone_block", SpiritStone1LQBlock::new);
        LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK = REGISTRY.register("low_quality_spirit_stone_ore_block", SpiritStone1LQOreBlock::new);
        LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("low_quality_spirit_stone_deepslate_ore_block", SpiritStone1LQDeepslateOreBlock::new);
        LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("low_quality_spirit_stone_compressed_block", SpiritStone1LQCompressedBlock::new);
        MEDIUM_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("medium_quality_spirit_stone_block", SpiritStone2MQBlock::new);
        MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK = REGISTRY.register("medium_quality_spirit_stone_ore_block", SpiritStone2MQOreBlock::new);
        MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("medium_quality_spirit_stone_deepslate_ore_block", SpiritStone2MQDeepslateOreBlock::new);
        MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("medium_quality_spirit_stone_compressed_block", SpiritStone2MQCompressedBlock::new);
        HIGH_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("high_quality_spirit_stone_block", SpiritStone3HQBlock::new);
        HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("high_quality_spirit_stone_deepslate_ore_block", SpiritStone3HQDeepslateOreBlock::new);
        HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("high_quality_spirit_stone_compressed_block", SpiritStone3HQCompressedBlock::new);
        TOP_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("top_quality_spirit_stone_block", SpiritStone4TQBlock::new);
        TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("top_quality_spirit_stone_deepslate_ore_block", SpiritStone4TQDeepslateOreBlock::new);
        TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("top_quality_spirit_stone_compressed_block", SpiritStone4TQCompressedBlock::new);

    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = REGISTRY.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ICModItems.REGISTRY.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
