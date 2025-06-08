package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.block.metals.*;
import net.hyper.immortal_cultivation.block.spirit_stones.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final DeferredBlock<Block> EARTH_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> FIRE_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> METAL_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> SKY_IRON_ORE_BLOCK;
    public static final DeferredBlock<Block> WATER_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> WATER_ASPECTED_DEEPSLATE_ORE_BLOCK;


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
        EARTH_ASPECTED_ORE_BLOCK = REGISTRY.register("earth_aspected_ore_block", EarthAspectedOreBlock::new);
        FIRE_ASPECTED_ORE_BLOCK = REGISTRY.register("fire_aspected_ore_block", FireAspectedOreBlock::new);
        FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK = REGISTRY.register("fire_aspected_deepslate_ore_block", FireAspectedDeepslateOreBlock::new);
        METAL_ASPECTED_DEEPSLATE_ORE_BLOCK = REGISTRY.register("metal_aspected_deepslate_ore_block", MetalAspectedDeepslateOreBlock::new);
        SKY_IRON_ORE_BLOCK = REGISTRY.register("sky_iron_ore_block", SkyIronOreBlock::new);
        WATER_ASPECTED_ORE_BLOCK = REGISTRY.register("water_aspected_ore_block", WaterAspectedOreBlock::new);
        WATER_ASPECTED_DEEPSLATE_ORE_BLOCK = REGISTRY.register("water_aspected_deepslate_ore_block", WaterAspectedDeepslateOreBlock::new);
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
