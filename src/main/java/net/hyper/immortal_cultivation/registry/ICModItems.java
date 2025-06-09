package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.item.metals.*;
import net.hyper.immortal_cultivation.item.spirit_stones.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems("immortal_cultivation");

    // Spirit Stone Blocks
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE_BLOCK;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK;

    // Ore Blocks
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK;
    public static final DeferredItem<Item> EARTH_ASPECTED_ORE_BLOCK;
    public static final DeferredItem<Item> FIRE_ASPECTED_ORE_BLOCK;
    public static final DeferredItem<Item> WATER_ASPECTED_ORE_BLOCK;
    public static final DeferredItem<Item> SKY_IRON_ORE_BLOCK;

    // Deepslate Ore Blocks
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> METAL_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> WATER_ASPECTED_DEEPSLATE_ORE_BLOCK;

    // Spirit Stones
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE;

    // Ores
    public static final DeferredItem<Item> EARTH_ASPECTED_ORE;
    public static final DeferredItem<Item> FIRE_ASPECTED_ORE;
    public static final DeferredItem<Item> METAL_ASPECTED_ORE;
    public static final DeferredItem<Item> SKY_IRON_ORE;
    public static final DeferredItem<Item> WATER_ASPECTED_ORE;
    public static final DeferredItem<Item> WOOD_ASPECTED_ORE;

    // Ingots
    public static final DeferredItem<Item> EARTH_ASPECTED_INGOT;
    public static final DeferredItem<Item> FIRE_ASPECTED_INGOT;
    public static final DeferredItem<Item> METAL_ASPECTED_INGOT;
    public static final DeferredItem<Item> WATER_ASPECTED_INGOT;
    public static final DeferredItem<Item> WOOD_ASPECTED_INGOT;
    public static final DeferredItem<Item> SKY_IRON_INGOT;

    // Swords
    public static final DeferredItem<SwordItem> EARTH_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> FIRE_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> METAL_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> SKY_IRON_SWORD;
    public static final DeferredItem<SwordItem> WATER_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> WOOD_ASPECTED_SWORD;

    // Pickaxes
    public static final DeferredItem<PickaxeItem> EARTH_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> FIRE_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> METAL_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> SKY_IRON_PICKAXE;
    public static final DeferredItem<PickaxeItem> WATER_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> WOOD_ASPECTED_PICKAXE;

    // Axes
    public static final DeferredItem<AxeItem> EARTH_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> FIRE_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> METAL_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> SKY_IRON_AXE;
    public static final DeferredItem<AxeItem> WATER_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> WOOD_ASPECTED_AXE;

    // Shovels
    public static final DeferredItem<ShovelItem> EARTH_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> FIRE_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> METAL_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> SKY_IRON_SHOVEL;
    public static final DeferredItem<ShovelItem> WATER_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> WOOD_ASPECTED_SHOVEL;

    // Hoes
    public static final DeferredItem<HoeItem> EARTH_ASPECTED_HOE;
    public static final DeferredItem<HoeItem> FIRE_ASPECTED_HOE;
    public static final DeferredItem<HoeItem> METAL_ASPECTED_HOE;
    public static final DeferredItem<HoeItem> SKY_IRON_HOE;
    public static final DeferredItem<HoeItem> WATER_ASPECTED_HOE;
    public static final DeferredItem<HoeItem> WOOD_ASPECTED_HOE;

    private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem((Block)block.get(), new Item.Properties()));
    }

    static {

        // Spirit Stone Block Items
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

        // Ore Block Items
        EARTH_ASPECTED_ORE_BLOCK = block(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK);
        FIRE_ASPECTED_ORE_BLOCK = block(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK);
        WATER_ASPECTED_ORE_BLOCK = block(ICModBlocks.WATER_ASPECTED_ORE_BLOCK);
        SKY_IRON_ORE_BLOCK = block(ICModBlocks.SKY_IRON_ORE_BLOCK);

        // Deepslate Ore Block Items
        FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK);
        METAL_ASPECTED_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK);
        WATER_ASPECTED_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK);

        // Spirit Stones
        LOW_QUALITY_SPIRIT_STONE = REGISTRY.register("low_quality_spirit_stone", () -> R1SpiritStoneItems.LOW_QUALITY_SPIRIT_STONE);
        MEDIUM_QUALITY_SPIRIT_STONE = REGISTRY.register("medium_quality_spirit_stone", () -> R1SpiritStoneItems.MEDIUM_QUALITY_SPIRIT_STONE);
        HIGH_QUALITY_SPIRIT_STONE = REGISTRY.register("high_quality_spirit_stone", () -> R1SpiritStoneItems.HIGH_QUALITY_SPIRIT_STONE);
        TOP_QUALITY_SPIRIT_STONE = REGISTRY.register("top_quality_spirit_stone", () -> R1SpiritStoneItems.TOP_QUALITY_SPIRIT_STONE);

        // Ores
        EARTH_ASPECTED_ORE = REGISTRY.register("earth_aspected_ore", () -> EarthAspectedItems.EARTH_ASPECTED_ORE);
        FIRE_ASPECTED_ORE = REGISTRY.register("fire_aspected_ore", () -> FireAspectedItems.FIRE_ASPECTED_ORE);
        METAL_ASPECTED_ORE = REGISTRY.register("metal_aspected_ore", () -> MetalAspectedItems.METAL_ASPECTED_ORE);
        WATER_ASPECTED_ORE = REGISTRY.register("water_aspected_ore", () -> WaterAspectedItems.WATER_ASPECTED_ORE);
        WOOD_ASPECTED_ORE = REGISTRY.register("wood_aspected_ore", () -> WoodAspectedItems.WOOD_ASPECTED_ORE);
        SKY_IRON_ORE = REGISTRY.register("sky_iron_ore", () -> SkyIronItems.SKY_IRON_ORE);

        // Ingots
        EARTH_ASPECTED_INGOT = REGISTRY.register("earth_aspected_ingot", () -> EarthAspectedItems.EARTH_ASPECTED_INGOT);
        FIRE_ASPECTED_INGOT = REGISTRY.register("fire_aspected_ingot", () -> FireAspectedItems.FIRE_ASPECTED_INGOT);
        METAL_ASPECTED_INGOT = REGISTRY.register("metal_aspected_ingot", () -> MetalAspectedItems.METAL_ASPECTED_INGOT);
        WATER_ASPECTED_INGOT = REGISTRY.register("water_aspected_ingot", () -> WaterAspectedItems.WATER_ASPECTED_INGOT);
        WOOD_ASPECTED_INGOT = REGISTRY.register("wood_aspected_ingot", () -> WoodAspectedItems.WOOD_ASPECTED_INGOT);
        SKY_IRON_INGOT = REGISTRY.register("sky_iron_ingot", () -> SkyIronItems.SKY_IRON_INGOT);

        // Swords
        EARTH_ASPECTED_SWORD = REGISTRY.register("earth_aspected_sword", () -> (SwordItem) EarthAspectedItems.EARTH_ASPECTED_SWORD);
        FIRE_ASPECTED_SWORD = REGISTRY.register("fire_aspected_sword", () -> (SwordItem) FireAspectedItems.FIRE_ASPECTED_SWORD);
        METAL_ASPECTED_SWORD = REGISTRY.register("metal_aspected_sword", () -> (SwordItem) MetalAspectedItems.METAL_ASPECTED_SWORD);
        SKY_IRON_SWORD = REGISTRY.register("sky_iron_sword", () -> (SwordItem) SkyIronItems.SKY_IRON_SWORD);
        WATER_ASPECTED_SWORD = REGISTRY.register("water_aspected_sword", () -> (SwordItem) WaterAspectedItems.WATER_ASPECTED_SWORD);
        WOOD_ASPECTED_SWORD = REGISTRY.register("wood_aspected_sword", () -> (SwordItem) WoodAspectedItems.WOOD_ASPECTED_SWORD);

        // Pickaxes
        EARTH_ASPECTED_PICKAXE = REGISTRY.register("earth_aspected_pickaxe", () -> (PickaxeItem) EarthAspectedItems.EARTH_ASPECTED_PICKAXE);
        FIRE_ASPECTED_PICKAXE = REGISTRY.register("fire_aspected_pickaxe", () -> (PickaxeItem) FireAspectedItems.FIRE_ASPECTED_PICKAXE);
        METAL_ASPECTED_PICKAXE = REGISTRY.register("metal_aspected_pickaxe", () -> (PickaxeItem) MetalAspectedItems.METAL_ASPECTED_PICKAXE);
        SKY_IRON_PICKAXE = REGISTRY.register("sky_iron_pickaxe", () -> (PickaxeItem) SkyIronItems.SKY_IRON_PICKAXE);
        WATER_ASPECTED_PICKAXE = REGISTRY.register("water_aspected_pickaxe", () -> (PickaxeItem) WaterAspectedItems.WATER_ASPECTED_PICKAXE);
        WOOD_ASPECTED_PICKAXE = REGISTRY.register("wood_aspected_pickaxe", () -> (PickaxeItem) WoodAspectedItems.WOOD_ASPECTED_PICKAXE);

        // Axes
        EARTH_ASPECTED_AXE = REGISTRY.register("earth_aspected_axe", () -> (AxeItem) EarthAspectedItems.EARTH_ASPECTED_AXE);
        FIRE_ASPECTED_AXE = REGISTRY.register("fire_aspected_axe", () -> (AxeItem) FireAspectedItems.FIRE_ASPECTED_AXE);
        METAL_ASPECTED_AXE = REGISTRY.register("metal_aspected_axe", () -> (AxeItem) MetalAspectedItems.METAL_ASPECTED_AXE);
        SKY_IRON_AXE = REGISTRY.register("sky_iron_axe", () -> (AxeItem) SkyIronItems.SKY_IRON_AXE);
        WATER_ASPECTED_AXE = REGISTRY.register("water_aspected_axe", () -> (AxeItem) WaterAspectedItems.WATER_ASPECTED_AXE);
        WOOD_ASPECTED_AXE = REGISTRY.register("wood_aspected_axe", () -> (AxeItem) WoodAspectedItems.WOOD_ASPECTED_AXE);

        // Shovels
        EARTH_ASPECTED_SHOVEL = REGISTRY.register("earth_aspected_shovel", () -> (ShovelItem) EarthAspectedItems.EARTH_ASPECTED_SHOVEL);
        FIRE_ASPECTED_SHOVEL = REGISTRY.register("fire_aspected_shovel", () -> (ShovelItem) FireAspectedItems.FIRE_ASPECTED_SHOVEL);
        METAL_ASPECTED_SHOVEL = REGISTRY.register("metal_aspected_shovel", () -> (ShovelItem) MetalAspectedItems.METAL_ASPECTED_SHOVEL);
        SKY_IRON_SHOVEL = REGISTRY.register("sky_iron_shovel", () -> (ShovelItem) SkyIronItems.SKY_IRON_SHOVEL);
        WATER_ASPECTED_SHOVEL = REGISTRY.register("water_aspected_shovel", () -> (ShovelItem) WaterAspectedItems.WATER_ASPECTED_SHOVEL);
        WOOD_ASPECTED_SHOVEL = REGISTRY.register("wood_aspected_shovel", () -> (ShovelItem) WoodAspectedItems.WOOD_ASPECTED_SHOVEL);

        // Hoes
        EARTH_ASPECTED_HOE = REGISTRY.register("earth_aspected_hoe", () -> (HoeItem) EarthAspectedItems.EARTH_ASPECTED_HOE);
        FIRE_ASPECTED_HOE = REGISTRY.register("fire_aspected_hoe", () -> (HoeItem) FireAspectedItems.FIRE_ASPECTED_HOE);
        METAL_ASPECTED_HOE = REGISTRY.register("metal_aspected_hoe", () -> (HoeItem) MetalAspectedItems.METAL_ASPECTED_HOE);
        SKY_IRON_HOE = REGISTRY.register("sky_iron_hoe", () -> (HoeItem) SkyIronItems.SKY_IRON_HOE);
        WATER_ASPECTED_HOE = REGISTRY.register("water_aspected_hoe", () -> (HoeItem) WaterAspectedItems.WATER_ASPECTED_HOE);
        WOOD_ASPECTED_HOE = REGISTRY.register("wood_aspected_hoe", () -> (HoeItem) WoodAspectedItems.WOOD_ASPECTED_HOE);

    }

}

