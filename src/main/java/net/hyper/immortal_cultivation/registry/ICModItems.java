package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.item.metals.*;
import net.hyper.immortal_cultivation.item.spirit_stones.SpiritStone1LQItem;
import net.hyper.immortal_cultivation.item.spirit_stones.SpiritStone2MQItem;
import net.hyper.immortal_cultivation.item.spirit_stones.SpiritStone3HQItem;
import net.hyper.immortal_cultivation.item.spirit_stones.SpiritStone4TQItem;
import net.hyper.immortal_cultivation.item.tools.EarthAspectedSwordItem;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems("immortal_cultivation");

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
    public static final DeferredItem<Item> EARTH_ASPECTED_ORE_BLOCK;
    public static final DeferredItem<Item> FIRE_ASPECTED_ORE_BLOCK;
    public static final DeferredItem<Item> FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> METAL_ASPECTED_DEEPSLATE_ORE_BLOCK;
    public static final DeferredItem<Item> SKY_IRON_ORE_BLOCK;
    public static final DeferredItem<Item> WATER_ASPECTED_ORE_BLOCK;
    public static final DeferredItem<Item> WATER_ASPECTED_DEEPSLATE_ORE_BLOCK;

    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> EARTH_ASPECTED_ORE;
    public static final DeferredItem<Item> EARTH_ASPECTED_INGOT;
    public static final DeferredItem<Item> FIRE_ASPECTED_ORE;
    public static final DeferredItem<Item> FIRE_ASPECTED_INGOT;
    public static final DeferredItem<Item> METAL_ASPECTED_ORE;
    public static final DeferredItem<Item> METAL_ASPECTED_INGOT;
    public static final DeferredItem<Item> SKY_IRON_ORE;
    public static final DeferredItem<Item> SKY_IRON_INGOT;
    public static final DeferredItem<Item> WATER_ASPECTED_ORE;
    public static final DeferredItem<Item> WATER_ASPECTED_INGOT;
    public static final DeferredItem<Item> WOOD_ASPECTED_ORE;
    public static final DeferredItem<Item> WOOD_ASPECTED_INGOT;

    public static final DeferredItem<SwordItem> EARTH_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> FIRE_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> METAL_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> SKY_IRON_SWORD;
    public static final DeferredItem<SwordItem> WATER_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> WOOD_ASPECTED_SWORD;

    public static final DeferredItem<PickaxeItem> EARTH_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> FIRE_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> METAL_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> SKY_IRON_PICKAXE;
    public static final DeferredItem<PickaxeItem> WATER_ASPECTED_PICKAXE;
    public static final DeferredItem<PickaxeItem> WOOD_ASPECTED_PICKAXE;

    public static final DeferredItem<AxeItem> EARTH_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> FIRE_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> METAL_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> SKY_IRON_AXE;
    public static final DeferredItem<AxeItem> WATER_ASPECTED_AXE;
    public static final DeferredItem<AxeItem> WOOD_ASPECTED_AXE;

    public static final DeferredItem<ShovelItem> EARTH_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> FIRE_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> METAL_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> SKY_IRON_SHOVEL;
    public static final DeferredItem<ShovelItem> WATER_ASPECTED_SHOVEL;
    public static final DeferredItem<ShovelItem> WOOD_ASPECTED_SHOVEL;

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
        EARTH_ASPECTED_ORE_BLOCK = block(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK);
        FIRE_ASPECTED_ORE_BLOCK = block(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK);
        FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK);
        METAL_ASPECTED_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK);
        SKY_IRON_ORE_BLOCK = block(ICModBlocks.SKY_IRON_ORE_BLOCK);
        WATER_ASPECTED_ORE_BLOCK = block(ICModBlocks.WATER_ASPECTED_ORE_BLOCK);
        WATER_ASPECTED_DEEPSLATE_ORE_BLOCK = block(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK);

        LOW_QUALITY_SPIRIT_STONE = REGISTRY.register("low_quality_spirit_stone", SpiritStone1LQItem::new);
        MEDIUM_QUALITY_SPIRIT_STONE = REGISTRY.register("medium_quality_spirit_stone", SpiritStone2MQItem::new);
        HIGH_QUALITY_SPIRIT_STONE = REGISTRY.register("high_quality_spirit_stone", SpiritStone3HQItem::new);
        TOP_QUALITY_SPIRIT_STONE = REGISTRY.register("top_quality_spirit_stone", SpiritStone4TQItem::new);
        EARTH_ASPECTED_ORE = REGISTRY.register("earth_aspected_ore", EarthAspectedOreItem::new);
        EARTH_ASPECTED_INGOT = REGISTRY.register("earth_aspected_ingot", EarthAspectedIngotItem::new);
        FIRE_ASPECTED_ORE = REGISTRY.register("fire_aspected_ore", FireAspectedOreItem::new);
        FIRE_ASPECTED_INGOT = REGISTRY.register("fire_aspected_ingot", FireAspectedIngotItem::new);
        METAL_ASPECTED_ORE = REGISTRY.register("metal_aspected_ore", MetalAspectedOreItem::new);
        METAL_ASPECTED_INGOT = REGISTRY.register("metal_aspected_ingot", MetalAspectedIngotItem::new);
        SKY_IRON_ORE = REGISTRY.register("sky_iron_ore", SkyIronOreItem::new);
        SKY_IRON_INGOT = REGISTRY.register("sky_iron_ingot", SkyIronIngotItem::new);
        WATER_ASPECTED_ORE = REGISTRY.register("water_aspected_ore", WaterAspectedOreItem::new);
        WATER_ASPECTED_INGOT = REGISTRY.register("water_aspected_ingot", WaterAspectedIngotItem::new);
        WOOD_ASPECTED_ORE = REGISTRY.register("wood_aspected_ore", WoodAspectedOreItem::new);
        WOOD_ASPECTED_INGOT = REGISTRY.register("wood_aspected_ingot", WoodAspectedIngotItem::new);

        EARTH_ASPECTED_SWORD = REGISTRY.register("earth_aspected_sword", EarthAspectedSwordItem::new);

    }

}

