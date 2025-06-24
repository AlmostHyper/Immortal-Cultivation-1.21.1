package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.item.crops.*;
import net.hyper.immortalcultivation.item.metals.realm1.*;
import net.hyper.immortalcultivation.item.pills.CultivationPillItems;
import net.hyper.immortalcultivation.item.spirit_stones.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems("immortalcultivation");

    // Spirit Stones
    public static final DeferredItem<Item> LOW_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> MEDIUM_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> HIGH_QUALITY_SPIRIT_STONE;
    public static final DeferredItem<Item> TOP_QUALITY_SPIRIT_STONE;

    // Ores
    public static final DeferredItem<Item> EARTH_ASPECTED_ORE;
    public static final DeferredItem<Item> FIRE_ASPECTED_ORE;
    public static final DeferredItem<Item> METAL_ASPECTED_ORE;
    public static final DeferredItem<Item> WATER_ASPECTED_ORE;
    public static final DeferredItem<Item> WOOD_ASPECTED_ORE;
    public static final DeferredItem<Item> SKY_IRON_ORE;

    // Ingots
    public static final DeferredItem<Item> EARTH_ASPECTED_INGOT;
    public static final DeferredItem<Item> FIRE_ASPECTED_INGOT;
    public static final DeferredItem<Item> METAL_ASPECTED_INGOT;
    public static final DeferredItem<Item> WATER_ASPECTED_INGOT;
    public static final DeferredItem<Item> WOOD_ASPECTED_INGOT;
    public static final DeferredItem<Item> SKY_IRON_INGOT;

    // === TOOLS === //

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

    // === WEAPONS === //

    // Swords
    public static final DeferredItem<SwordItem> EARTH_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> FIRE_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> METAL_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> SKY_IRON_SWORD;
    public static final DeferredItem<SwordItem> WATER_ASPECTED_SWORD;
    public static final DeferredItem<SwordItem> WOOD_ASPECTED_SWORD;

    // === ARMOR === //

    // Helmets
    public static final DeferredItem<ArmorItem> EARTH_ASPECTED_HELMET;
    public static final DeferredItem<ArmorItem> FIRE_ASPECTED_HELMET;
    public static final DeferredItem<ArmorItem> METAL_ASPECTED_HELMET;
    public static final DeferredItem<ArmorItem> WATER_ASPECTED_HELMET;
    public static final DeferredItem<ArmorItem> WOOD_ASPECTED_HELMET;
    public static final DeferredItem<ArmorItem> SKY_IRON_HELMET;

    // Chestplates
    public static final DeferredItem<ArmorItem> EARTH_ASPECTED_CHESTPLATE;
    public static final DeferredItem<ArmorItem> FIRE_ASPECTED_CHESTPLATE;
    public static final DeferredItem<ArmorItem> METAL_ASPECTED_CHESTPLATE;
    public static final DeferredItem<ArmorItem> WATER_ASPECTED_CHESTPLATE;
    public static final DeferredItem<ArmorItem> WOOD_ASPECTED_CHESTPLATE;
    public static final DeferredItem<ArmorItem> SKY_IRON_CHESTPLATE;

    // Leggings
    public static final DeferredItem<ArmorItem> EARTH_ASPECTED_LEGGINGS;
    public static final DeferredItem<ArmorItem> FIRE_ASPECTED_LEGGINGS;
    public static final DeferredItem<ArmorItem> METAL_ASPECTED_LEGGINGS;
    public static final DeferredItem<ArmorItem> WATER_ASPECTED_LEGGINGS;
    public static final DeferredItem<ArmorItem> WOOD_ASPECTED_LEGGINGS;
    public static final DeferredItem<ArmorItem> SKY_IRON_LEGGINGS;

    // Boots
    public static final DeferredItem<ArmorItem> EARTH_ASPECTED_BOOTS;
    public static final DeferredItem<ArmorItem> FIRE_ASPECTED_BOOTS;
    public static final DeferredItem<ArmorItem> METAL_ASPECTED_BOOTS;
    public static final DeferredItem<ArmorItem> WATER_ASPECTED_BOOTS;
    public static final DeferredItem<ArmorItem> WOOD_ASPECTED_BOOTS;
    public static final DeferredItem<ArmorItem> SKY_IRON_BOOTS;

    // === PLANTS === //

    // Earth Plants
    public static final DeferredItem<Item> YELLOW_EARTH_ROOTS;

    // Fire Plants
    public static final DeferredItem<Item> RED_FIRE_ROOTS;

    // Metal Plants
    public static final DeferredItem<Item> WHITE_METAL_ROOTS;

    // Water Plants
    public static final DeferredItem<Item> BLACK_WATER_ROOTS;

    // Wood Plants
    public static final DeferredItem<Item> BLUE_WOOD_ROOTS;

    // Lightning Plants

    // Wind Plants

    // Poison Plants

    // === PILLS === //

    // Qi Cultivation Pills
    public static final DeferredItem<Item> LOW_QUALITY_QI_CULTIVATION_PILL;
    public static final DeferredItem<Item> MEDIUM_QUALITY_QI_CULTIVATION_PILL;
    public static final DeferredItem<Item> HIGH_QUALITY_QI_CULTIVATION_PILL;
    public static final DeferredItem<Item> TOP_QUALITY_QI_CULTIVATION_PILL;

    // Body Cultivation Pills
    public static final DeferredItem<Item> LOW_QUALITY_BODY_CULTIVATION_PILL;
    public static final DeferredItem<Item> MEDIUM_QUALITY_BODY_CULTIVATION_PILL;
    public static final DeferredItem<Item> HIGH_QUALITY_BODY_CULTIVATION_PILL;
    public static final DeferredItem<Item> TOP_QUALITY_BODY_CULTIVATION_PILL;

    static {
        // Spirit Stones
        LOW_QUALITY_SPIRIT_STONE = REGISTRY.register("low_quality_spirit_stone", R1SpiritStoneItems.LOW_QUALITY_SPIRIT_STONE());
        MEDIUM_QUALITY_SPIRIT_STONE = REGISTRY.register("medium_quality_spirit_stone", R1SpiritStoneItems.MEDIUM_QUALITY_SPIRIT_STONE());
        HIGH_QUALITY_SPIRIT_STONE = REGISTRY.register("high_quality_spirit_stone", R1SpiritStoneItems.HIGH_QUALITY_SPIRIT_STONE());
        TOP_QUALITY_SPIRIT_STONE = REGISTRY.register("top_quality_spirit_stone", R1SpiritStoneItems.TOP_QUALITY_SPIRIT_STONE());

        // Ores
        EARTH_ASPECTED_ORE = REGISTRY.register("earth_aspected_ore", EarthAspectedItems.EARTH_ASPECTED_ORE());
        FIRE_ASPECTED_ORE = REGISTRY.register("fire_aspected_ore", FireAspectedItems.FIRE_ASPECTED_ORE());
        METAL_ASPECTED_ORE = REGISTRY.register("metal_aspected_ore", MetalAspectedItems.METAL_ASPECTED_ORE());
        WATER_ASPECTED_ORE = REGISTRY.register("water_aspected_ore", WaterAspectedItems.WATER_ASPECTED_ORE());
        WOOD_ASPECTED_ORE = REGISTRY.register("wood_aspected_ore", WoodAspectedItems.WOOD_ASPECTED_ORE());
        SKY_IRON_ORE = REGISTRY.register("sky_iron_ore", SkyIronItems.SKY_IRON_ORE());

        // Ingots
        EARTH_ASPECTED_INGOT = REGISTRY.register("earth_aspected_ingot", EarthAspectedItems.EARTH_ASPECTED_INGOT());
        FIRE_ASPECTED_INGOT = REGISTRY.register("fire_aspected_ingot", FireAspectedItems.FIRE_ASPECTED_INGOT());
        METAL_ASPECTED_INGOT = REGISTRY.register("metal_aspected_ingot", MetalAspectedItems.METAL_ASPECTED_INGOT());
        WATER_ASPECTED_INGOT = REGISTRY.register("water_aspected_ingot", WaterAspectedItems.WATER_ASPECTED_INGOT());
        WOOD_ASPECTED_INGOT = REGISTRY.register("wood_aspected_ingot", WoodAspectedItems.WOOD_ASPECTED_INGOT());
        SKY_IRON_INGOT = REGISTRY.register("sky_iron_ingot", SkyIronItems.SKY_IRON_INGOT());

        // Pickaxes
        EARTH_ASPECTED_PICKAXE = REGISTRY.register("earth_aspected_pickaxe", EarthAspectedItems.EARTH_ASPECTED_PICKAXE());
        FIRE_ASPECTED_PICKAXE = REGISTRY.register("fire_aspected_pickaxe", FireAspectedItems.FIRE_ASPECTED_PICKAXE());
        METAL_ASPECTED_PICKAXE = REGISTRY.register("metal_aspected_pickaxe", MetalAspectedItems.METAL_ASPECTED_PICKAXE());
        WATER_ASPECTED_PICKAXE = REGISTRY.register("water_aspected_pickaxe", WaterAspectedItems.WATER_ASPECTED_PICKAXE());
        WOOD_ASPECTED_PICKAXE = REGISTRY.register("wood_aspected_pickaxe", WoodAspectedItems.WOOD_ASPECTED_PICKAXE());
        SKY_IRON_PICKAXE = REGISTRY.register("sky_iron_pickaxe", SkyIronItems.SKY_IRON_PICKAXE());

        // Axes
        EARTH_ASPECTED_AXE = REGISTRY.register("earth_aspected_axe", EarthAspectedItems.EARTH_ASPECTED_AXE());
        FIRE_ASPECTED_AXE = REGISTRY.register("fire_aspected_axe", FireAspectedItems.FIRE_ASPECTED_AXE());
        METAL_ASPECTED_AXE = REGISTRY.register("metal_aspected_axe", MetalAspectedItems.METAL_ASPECTED_AXE());
        WATER_ASPECTED_AXE = REGISTRY.register("water_aspected_axe", WaterAspectedItems.WATER_ASPECTED_AXE());
        WOOD_ASPECTED_AXE = REGISTRY.register("wood_aspected_axe", WoodAspectedItems.WOOD_ASPECTED_AXE());
        SKY_IRON_AXE = REGISTRY.register("sky_iron_axe", SkyIronItems.SKY_IRON_AXE());

        // Shovels
        EARTH_ASPECTED_SHOVEL = REGISTRY.register("earth_aspected_shovel", EarthAspectedItems.EARTH_ASPECTED_SHOVEL());
        FIRE_ASPECTED_SHOVEL = REGISTRY.register("fire_aspected_shovel", FireAspectedItems.FIRE_ASPECTED_SHOVEL());
        METAL_ASPECTED_SHOVEL = REGISTRY.register("metal_aspected_shovel", MetalAspectedItems.METAL_ASPECTED_SHOVEL());
        WATER_ASPECTED_SHOVEL = REGISTRY.register("water_aspected_shovel", WaterAspectedItems.WATER_ASPECTED_SHOVEL());
        WOOD_ASPECTED_SHOVEL = REGISTRY.register("wood_aspected_shovel", WoodAspectedItems.WOOD_ASPECTED_SHOVEL());
        SKY_IRON_SHOVEL = REGISTRY.register("sky_iron_shovel", SkyIronItems.SKY_IRON_SHOVEL());

        // Hoes
        EARTH_ASPECTED_HOE = REGISTRY.register("earth_aspected_hoe", EarthAspectedItems.EARTH_ASPECTED_HOE());
        FIRE_ASPECTED_HOE = REGISTRY.register("fire_aspected_hoe", FireAspectedItems.FIRE_ASPECTED_HOE());
        METAL_ASPECTED_HOE = REGISTRY.register("metal_aspected_hoe", MetalAspectedItems.METAL_ASPECTED_HOE());
        WATER_ASPECTED_HOE = REGISTRY.register("water_aspected_hoe", WaterAspectedItems.WATER_ASPECTED_HOE());
        WOOD_ASPECTED_HOE = REGISTRY.register("wood_aspected_hoe", WoodAspectedItems.WOOD_ASPECTED_HOE());
        SKY_IRON_HOE = REGISTRY.register("sky_iron_hoe", SkyIronItems.SKY_IRON_HOE());

        // Swords
        EARTH_ASPECTED_SWORD = REGISTRY.register("earth_aspected_sword", EarthAspectedItems.EARTH_ASPECTED_SWORD());
        FIRE_ASPECTED_SWORD = REGISTRY.register("fire_aspected_sword", FireAspectedItems.FIRE_ASPECTED_SWORD());
        METAL_ASPECTED_SWORD = REGISTRY.register("metal_aspected_sword", MetalAspectedItems.METAL_ASPECTED_SWORD());
        WATER_ASPECTED_SWORD = REGISTRY.register("water_aspected_sword", WaterAspectedItems.WATER_ASPECTED_SWORD());
        WOOD_ASPECTED_SWORD = REGISTRY.register("wood_aspected_sword", WoodAspectedItems.WOOD_ASPECTED_SWORD());
        SKY_IRON_SWORD = REGISTRY.register("sky_iron_sword", SkyIronItems.SKY_IRON_SWORD());

        // Helmets
        EARTH_ASPECTED_HELMET = REGISTRY.register("earth_aspected_helmet", EarthAspectedItems.EARTH_ASPECTED_HELMET());
        FIRE_ASPECTED_HELMET = REGISTRY.register("fire_aspected_helmet", FireAspectedItems.FIRE_ASPECTED_HELMET());
        METAL_ASPECTED_HELMET = REGISTRY.register("metal_aspected_helmet", MetalAspectedItems.METAL_ASPECTED_HELMET());
        WATER_ASPECTED_HELMET = REGISTRY.register("water_aspected_helmet", WaterAspectedItems.WATER_ASPECTED_HELMET());
        WOOD_ASPECTED_HELMET = REGISTRY.register("wood_aspected_helmet", WoodAspectedItems.WOOD_ASPECTED_HELMET());
        SKY_IRON_HELMET = REGISTRY.register("sky_iron_helmet", SkyIronItems.SKY_IRON_HELMET());

        // Chestplates
        EARTH_ASPECTED_CHESTPLATE = REGISTRY.register("earth_aspected_chestplate", EarthAspectedItems.EARTH_ASPECTED_CHESTPLATE());
        FIRE_ASPECTED_CHESTPLATE = REGISTRY.register("fire_aspected_chestplate", FireAspectedItems.FIRE_ASPECTED_CHESTPLATE());
        METAL_ASPECTED_CHESTPLATE = REGISTRY.register("metal_aspected_chestplate", MetalAspectedItems.METAL_ASPECTED_CHESTPLATE());
        WATER_ASPECTED_CHESTPLATE = REGISTRY.register("water_aspected_chestplate", WaterAspectedItems.WATER_ASPECTED_CHESTPLATE());
        WOOD_ASPECTED_CHESTPLATE = REGISTRY.register("wood_aspected_chestplate", WoodAspectedItems.WOOD_ASPECTED_CHESTPLATE());
        SKY_IRON_CHESTPLATE = REGISTRY.register("sky_iron_chestplate", SkyIronItems.SKY_IRON_CHESTPLATE());

        // Leggings
        EARTH_ASPECTED_LEGGINGS = REGISTRY.register("earth_aspected_leggings", EarthAspectedItems.EARTH_ASPECTED_LEGGINGS());
        FIRE_ASPECTED_LEGGINGS = REGISTRY.register("fire_aspected_leggings", FireAspectedItems.FIRE_ASPECTED_LEGGINGS());
        METAL_ASPECTED_LEGGINGS = REGISTRY.register("metal_aspected_leggings", MetalAspectedItems.METAL_ASPECTED_LEGGINGS());
        WATER_ASPECTED_LEGGINGS = REGISTRY.register("water_aspected_leggings", WaterAspectedItems.WATER_ASPECTED_LEGGINGS());
        WOOD_ASPECTED_LEGGINGS = REGISTRY.register("wood_aspected_leggings", WoodAspectedItems.WOOD_ASPECTED_LEGGINGS());
        SKY_IRON_LEGGINGS = REGISTRY.register("sky_iron_leggings", SkyIronItems.SKY_IRON_LEGGINGS());

        // Boots
        EARTH_ASPECTED_BOOTS = REGISTRY.register("earth_aspected_boots", EarthAspectedItems.EARTH_ASPECTED_BOOTS());
        FIRE_ASPECTED_BOOTS = REGISTRY.register("fire_aspected_boots", FireAspectedItems.FIRE_ASPECTED_BOOTS());
        METAL_ASPECTED_BOOTS = REGISTRY.register("metal_aspected_boots", MetalAspectedItems.METAL_ASPECTED_BOOTS());
        WATER_ASPECTED_BOOTS = REGISTRY.register("water_aspected_boots", WaterAspectedItems.WATER_ASPECTED_BOOTS());
        WOOD_ASPECTED_BOOTS = REGISTRY.register("wood_aspected_boots", WoodAspectedItems.WOOD_ASPECTED_BOOTS());
        SKY_IRON_BOOTS = REGISTRY.register("sky_iron_boots", SkyIronItems.SKY_IRON_BOOTS());

        // === PLANTS === //

        // Earth Plants
        YELLOW_EARTH_ROOTS = REGISTRY.register("yellow_earth_roots", EarthCropItems.YELLOW_EARTH_ROOTS());

        // Fire Plants
        RED_FIRE_ROOTS = REGISTRY.register("red_fire_roots", FireCropItems.RED_FIRE_ROOTS());

        // Metal Plants
        WHITE_METAL_ROOTS = REGISTRY.register("white_metal_roots", MetalCropItems.WHITE_METAL_ROOTS());

        // Water Plants
        BLACK_WATER_ROOTS = REGISTRY.register("black_water_roots", WaterCropItems.BLACK_WATER_ROOTS());

        // Wood Plants
        BLUE_WOOD_ROOTS = REGISTRY.register("blue_wood_roots", WoodCropItems.BLUE_WOOD_ROOTS());

        // === PILLS === //

        // Qi Cultivation Pills
        LOW_QUALITY_QI_CULTIVATION_PILL = REGISTRY.register("low_quality_qi_absorption_pill", CultivationPillItems.LOW_QUALITY_QI_CULTIVATION_PILL());
        MEDIUM_QUALITY_QI_CULTIVATION_PILL = REGISTRY.register("medium_quality_qi_absorption_pill", CultivationPillItems.MEDIUM_QUALITY_QI_CULTIVATION_PILL());
        HIGH_QUALITY_QI_CULTIVATION_PILL = REGISTRY.register("high_quality_qi_absorption_pill", CultivationPillItems.HIGH_QUALITY_QI_CULTIVATION_PILL());
        TOP_QUALITY_QI_CULTIVATION_PILL = REGISTRY.register("top_quality_qi_absorption_pill", CultivationPillItems.TOP_QUALITY_QI_CULTIVATION_PILL());

        // Body Cultivation Pills
        LOW_QUALITY_BODY_CULTIVATION_PILL = REGISTRY.register("low_quality_body_absorption_pill", CultivationPillItems.LOW_QUALITY_BODY_CULTIVATION_PILL());
        MEDIUM_QUALITY_BODY_CULTIVATION_PILL = REGISTRY.register("medium_quality_body_absorption_pill", CultivationPillItems.MEDIUM_QUALITY_BODY_CULTIVATION_PILL());
        HIGH_QUALITY_BODY_CULTIVATION_PILL = REGISTRY.register("high_quality_body_absorption_pill", CultivationPillItems.HIGH_QUALITY_BODY_CULTIVATION_PILL());
        TOP_QUALITY_BODY_CULTIVATION_PILL = REGISTRY.register("top_quality_body_absorption_pill", CultivationPillItems.TOP_QUALITY_BODY_CULTIVATION_PILL());

    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }
}
