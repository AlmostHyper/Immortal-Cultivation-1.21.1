package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.block.metals.*;
import net.hyper.immortalcultivation.block.spirit_stones.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModBlocks {
    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks("immortalcultivation");

    // Spirit Stones
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;

    // Ores
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredBlock<Block> EARTH_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> FIRE_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> WATER_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> SKY_IRON_ORE_BLOCK;

    // Deepslate Ores
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> METAL_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> WATER_ASPECTED_DEEPSLATE_ORE_BLOCK;

    static {

        // Spirit Stones
        LOW_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("low_quality_spirit_stone_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK());
        LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("low_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("medium_quality_spirit_stone_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("medium_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());
        HIGH_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("high_quality_spirit_stone_block", R1SpiritStoneBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK());
        HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("high_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());
        TOP_QUALITY_SPIRIT_STONE_BLOCK = REGISTRY.register("top_quality_spirit_stone_block", R1SpiritStoneBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK());
        TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = REGISTRY.register("top_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());

        // Ores
        LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK = REGISTRY.register("low_quality_spirit_stone_ore_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK = REGISTRY.register("medium_quality_spirit_stone_ore_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK());
        EARTH_ASPECTED_ORE_BLOCK = REGISTRY.register("earth_aspected_ore_block", EarthAspectedBlocks.EARTH_ASPECTED_ORE_BLOCK());
        FIRE_ASPECTED_ORE_BLOCK = REGISTRY.register("fire_aspected_ore_block", FireAspectedBlocks.FIRE_ASPECTED_ORE_BLOCK());
        WATER_ASPECTED_ORE_BLOCK = REGISTRY.register("water_aspected_ore_block", WaterAspectedBlocks.WATER_ASPECTED_ORE_BLOCK());
        SKY_IRON_ORE_BLOCK = REGISTRY.register("sky_iron_ore_block", WoodAspectedBlocks.WOOD_ASPECTED_ORE_BLOCK());

        // Deepslate Ores
        LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("low_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("medium_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("high_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = REGISTRY.register("top_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK = REGISTRY.register("fire_aspected_deepslate_ore_block", FireAspectedBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK());
        METAL_ASPECTED_DEEPSLATE_ORE_BLOCK = REGISTRY.register("metal_aspected_deepslate_ore_block", MetalAspectedBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK());
        WATER_ASPECTED_DEEPSLATE_ORE_BLOCK = REGISTRY.register("water_aspected_deepslate_ore_block", WaterAspectedBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK());

    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
