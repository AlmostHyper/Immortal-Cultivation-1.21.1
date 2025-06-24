package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.block.crafting.IronPillFurnaceBlock;
import net.hyper.immortalcultivation.block.crafting.PillFurnaceBlock;
import net.hyper.immortalcultivation.block.crops.*;
import net.hyper.immortalcultivation.block.metals.*;
import net.hyper.immortalcultivation.block.spirit_stones.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

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
    public static final DeferredBlock<Block> METAL_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> WATER_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> WOOD_ASPECTED_ORE_BLOCK;
    public static final DeferredBlock<Block> SKY_IRON_ORE_BLOCK;

    // Deepslate Ores
    public static final DeferredBlock<Block> LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> METAL_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> WATER_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredBlock<Block> WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK;

    // === PLANTS === //

    // Earth Plants
    public static final DeferredBlock<Block> YELLOW_EARTH_ROOTS_CROP_BLOCK;

    // Fire Plants
    public static final DeferredBlock<Block> RED_FIRE_ROOTS_CROP_BLOCK;

    // Metal Plants
    public static final DeferredBlock<Block> WHITE_METAL_ROOTS_CROP_BLOCK;

    // Water Plants
    public static final DeferredBlock<Block> BLACK_WATER_ROOTS_CROP_BLOCK;

    // Wood Plants
    public static final DeferredBlock<Block> BLUE_WOOD_ROOTS_CROP_BLOCK;

    // === CRAFTING === //

    // Pill Furnaces
    public static final DeferredBlock<Block> PILL_FURNACE_BLOCK;


    static {
        // Spirit Stones
        LOW_QUALITY_SPIRIT_STONE_BLOCK = registerBlockWithItem("low_quality_spirit_stone_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK());
        LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = registerBlockWithItem("low_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_BLOCK = registerBlockWithItem("medium_quality_spirit_stone_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = registerBlockWithItem("medium_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());
        HIGH_QUALITY_SPIRIT_STONE_BLOCK = registerBlockWithItem("high_quality_spirit_stone_block", R1SpiritStoneBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK());
        HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = registerBlockWithItem("high_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());
        TOP_QUALITY_SPIRIT_STONE_BLOCK = registerBlockWithItem("top_quality_spirit_stone_block", R1SpiritStoneBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK());
        TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = registerBlockWithItem("top_quality_spirit_stone_compressed_block", R1SpiritStoneBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK());

        // Ores
        LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK = registerBlockWithItem("low_quality_spirit_stone_ore_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK = registerBlockWithItem("medium_quality_spirit_stone_ore_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK());
        EARTH_ASPECTED_ORE_BLOCK = registerBlockWithItem("earth_aspected_ore_block", EarthAspectedBlocks.EARTH_ASPECTED_ORE_BLOCK());
        FIRE_ASPECTED_ORE_BLOCK = registerBlockWithItem("fire_aspected_ore_block", FireAspectedBlocks.FIRE_ASPECTED_ORE_BLOCK());
        METAL_ASPECTED_ORE_BLOCK = registerBlockWithItem("metal_aspected_ore_block", MetalAspectedBlocks.METAL_ASPECTED_ORE_BLOCK());
        WATER_ASPECTED_ORE_BLOCK = registerBlockWithItem("water_aspected_ore_block", WaterAspectedBlocks.WATER_ASPECTED_ORE_BLOCK());
        WOOD_ASPECTED_ORE_BLOCK = registerBlockWithItem("wood_aspected_ore_block", WoodAspectedBlocks.WOOD_ASPECTED_ORE_BLOCK());
        SKY_IRON_ORE_BLOCK = registerBlockWithItem("sky_iron_ore_block", SkyIronBlocks.SKY_IRON_ORE_BLOCK());

        // Deepslate Ores
        LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("low_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("medium_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("high_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("top_quality_spirit_stone_deepslate_ore_block", R1SpiritStoneBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK());
        EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("earth_aspected_deepslate_ore_block", EarthAspectedBlocks.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK());
        FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("fire_aspected_deepslate_ore_block", FireAspectedBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK());
        METAL_ASPECTED_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("metal_aspected_deepslate_ore_block", MetalAspectedBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK());
        WATER_ASPECTED_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("water_aspected_deepslate_ore_block", WaterAspectedBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK());
        WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK = registerBlockWithItem("wood_aspected_deepslate_ore_block", WoodAspectedBlocks.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK());

        // === PLANTS == //

        // Earth Plants
        YELLOW_EARTH_ROOTS_CROP_BLOCK = registerBlockWithItem("yellow_earth_roots_crop_block", EarthCropBlock.YELLOW_EARTH_ROOTS());

        // Fire Plants
        RED_FIRE_ROOTS_CROP_BLOCK = registerBlockWithItem("red_fire_roots_crop_block", FireCropBlock.RED_FIRE_ROOTS());

        // Metal Plants
        WHITE_METAL_ROOTS_CROP_BLOCK = registerBlockWithItem("white_metal_roots_crop_block", MetalCropBlock.WHITE_METAL_ROOTS());

        // Water Plants
        BLACK_WATER_ROOTS_CROP_BLOCK = registerBlockWithItem("black_water_roots_crop_block", WaterCropBlock.BLACK_WATER_ROOTS());

        //  Plants
        BLUE_WOOD_ROOTS_CROP_BLOCK = registerBlockWithItem("blue_wood_roots_crop_block", WoodCropBlock.BLUE_WOOD_ROOTS());

        // === CRAFTING === //

        // Pill Furnaces
        PILL_FURNACE_BLOCK = registerBlockWithItem("pill_furnace_block", PillFurnaceBlock.PILL_FURNACE_BLOCK());
    }

    private static DeferredBlock<Block> registerBlockWithItem(String name, Supplier<? extends Block> blockSupplier, Item.Properties itemProperties) {
        DeferredBlock<Block> block = REGISTRY.register(name, blockSupplier);

        Item.Properties properties = itemProperties != null ? itemProperties : new Item.Properties();
        ICModItems.REGISTRY.register(name, () -> new BlockItem(block.get(), properties));

        return block;
    }

    private static DeferredBlock<Block> registerBlockWithItem(String name, Supplier<? extends Block> blockSupplier) {
        return registerBlockWithItem(name, blockSupplier, null);
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }
}

