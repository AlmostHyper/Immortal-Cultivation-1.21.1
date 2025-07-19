package net.hyper.immortalcultivation.datagen.recipes.helper;

import net.hyper.immortalcultivation.datagen.recipes.RecipeProvider;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ICModShapedRecipeHelper {

    public static void buildShapedRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        buildSpiritStoneRecipes(recipeOutput, helper);
        buildToolRecipes(recipeOutput, helper);
        buildArmorRecipes(recipeOutput, helper);
        buildPillFurnaceRecipes(recipeOutput, helper);
    }

    private static void buildSpiritStoneRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("LLL")
            .pattern("LLL")
            .pattern("LLL")
            .define('L', ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_low_quality_spirit_stone", helper.has(ItemRegistry.LOW_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("LLL")
            .pattern("LLL")
            .pattern("LLL")
            .define('L', BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_low_quality_spirit_stone_block", helper.has(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("MMM")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_medium_quality_spirit_stone", helper.has(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("MMM")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_medium_quality_spirit_stone_block", helper.has(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("HHH")
            .pattern("HHH")
            .pattern("HHH")
            .define('H', ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_high_quality_spirit_stone", helper.has(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("HHH")
            .pattern("HHH")
            .pattern("HHH")
            .define('H', BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_high_quality_spirit_stone_block", helper.has(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("TTT")
            .pattern("TTT")
            .pattern("TTT")
            .define('T', ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_top_quality_spirit_stone", helper.has(ItemRegistry.TOP_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("TTT")
            .pattern("TTT")
            .pattern("TTT")
            .define('T', BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_top_quality_spirit_stone_block", helper.has(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);
    }

    private static void buildToolRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        buildSwordRecipes(recipeOutput, helper);
        buildPickaxeRecipes(recipeOutput, helper);
        buildAxeRecipes(recipeOutput, helper);
        buildShovelRecipes(recipeOutput, helper);
        buildHoeRecipes(recipeOutput, helper);
    }

    private static void buildSwordRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.EARTH_ASPECTED_SWORD.get())
            .pattern("E")
            .pattern("E")
            .pattern("I")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.FIRE_ASPECTED_SWORD.get())
            .pattern("F")
            .pattern("F")
            .pattern("I")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.METAL_ASPECTED_SWORD.get())
            .pattern("M")
            .pattern("M")
            .pattern("I")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.WATER_ASPECTED_SWORD.get())
            .pattern("W")
            .pattern("W")
            .pattern("I")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.WOOD_ASPECTED_SWORD.get())
            .pattern("W")
            .pattern("W")
            .pattern("I")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SKY_IRON_SWORD.get())
            .pattern("S")
            .pattern("S")
            .pattern("I")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildPickaxeRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EARTH_ASPECTED_PICKAXE.get())
            .pattern("EEE")
            .pattern(" I ")
            .pattern(" I ")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.FIRE_ASPECTED_PICKAXE.get())
            .pattern("FFF")
            .pattern(" I ")
            .pattern(" I ")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.METAL_ASPECTED_PICKAXE.get())
            .pattern("MMM")
            .pattern(" I ")
            .pattern(" I ")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WATER_ASPECTED_PICKAXE.get())
            .pattern("WWW")
            .pattern(" I ")
            .pattern(" I ")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WOOD_ASPECTED_PICKAXE.get())
            .pattern("WWW")
            .pattern(" I ")
            .pattern(" I ")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.SKY_IRON_PICKAXE.get())
            .pattern("SSS")
            .pattern(" I ")
            .pattern(" I ")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildAxeRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EARTH_ASPECTED_AXE.get())
            .pattern("EE")
            .pattern("EI")
            .pattern(" I")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.FIRE_ASPECTED_AXE.get())
            .pattern("FF")
            .pattern("FI")
            .pattern(" I")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.METAL_ASPECTED_AXE.get())
            .pattern("MM")
            .pattern("MI")
            .pattern(" I")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WATER_ASPECTED_AXE.get())
            .pattern("WW")
            .pattern("WI")
            .pattern(" I")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WOOD_ASPECTED_AXE.get())
            .pattern("WW")
            .pattern("WI")
            .pattern(" I")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.SKY_IRON_AXE.get())
            .pattern("SS")
            .pattern("SI")
            .pattern(" I")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildShovelRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EARTH_ASPECTED_SHOVEL.get())
            .pattern("E")
            .pattern("I")
            .pattern("I")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.FIRE_ASPECTED_SHOVEL.get())
            .pattern("F")
            .pattern("I")
            .pattern("I")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.METAL_ASPECTED_SHOVEL.get())
            .pattern("M")
            .pattern("I")
            .pattern("I")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WATER_ASPECTED_SHOVEL.get())
            .pattern("W")
            .pattern("I")
            .pattern("I")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WOOD_ASPECTED_SHOVEL.get())
            .pattern("W")
            .pattern("I")
            .pattern("I")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.SKY_IRON_SHOVEL.get())
            .pattern("S")
            .pattern("I")
            .pattern("I")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildHoeRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EARTH_ASPECTED_HOE.get())
            .pattern("EE")
            .pattern(" I")
            .pattern(" I")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.FIRE_ASPECTED_HOE.get())
            .pattern("FF")
            .pattern(" I")
            .pattern(" I")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.METAL_ASPECTED_HOE.get())
            .pattern("MM")
            .pattern(" I")
            .pattern(" I")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WATER_ASPECTED_HOE.get())
            .pattern("WW")
            .pattern(" I")
            .pattern(" I")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WOOD_ASPECTED_HOE.get())
            .pattern("WW")
            .pattern(" I")
            .pattern(" I")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.SKY_IRON_HOE.get())
            .pattern("SS")
            .pattern(" I")
            .pattern(" I")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildArmorRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        // === ARMOR === //
        buildHelmetRecipes(recipeOutput, helper);
        buildChestplateRecipes(recipeOutput, helper);
        buildLeggingsRecipes(recipeOutput, helper);
        buildBootsRecipes(recipeOutput, helper);
    }

    private static void buildHelmetRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.EARTH_ASPECTED_HELMET.get())
            .pattern("EEE")
            .pattern("E E")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.FIRE_ASPECTED_HELMET.get())
            .pattern("FFF")
            .pattern("F F")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.METAL_ASPECTED_HELMET.get())
            .pattern("MMM")
            .pattern("M M")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WATER_ASPECTED_HELMET.get())
            .pattern("WWW")
            .pattern("W W")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WOOD_ASPECTED_HELMET.get())
            .pattern("WWW")
            .pattern("W W")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.SKY_IRON_HELMET.get())
            .pattern("SSS")
            .pattern("S S")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildChestplateRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.EARTH_ASPECTED_CHESTPLATE.get())
            .pattern("E E")
            .pattern("EEE")
            .pattern("EEE")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.FIRE_ASPECTED_CHESTPLATE.get())
            .pattern("F F")
            .pattern("FFF")
            .pattern("FFF")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.METAL_ASPECTED_CHESTPLATE.get())
            .pattern("M M")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WATER_ASPECTED_CHESTPLATE.get())
            .pattern("W W")
            .pattern("WWW")
            .pattern("WWW")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WOOD_ASPECTED_CHESTPLATE.get())
            .pattern("W W")
            .pattern("WWW")
            .pattern("WWW")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.SKY_IRON_CHESTPLATE.get())
            .pattern("S S")
            .pattern("SSS")
            .pattern("SSS")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildLeggingsRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.EARTH_ASPECTED_LEGGINGS.get())
            .pattern("EEE")
            .pattern("E E")
            .pattern("E E")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.FIRE_ASPECTED_LEGGINGS.get())
            .pattern("FFF")
            .pattern("F F")
            .pattern("F F")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.METAL_ASPECTED_LEGGINGS.get())
            .pattern("MMM")
            .pattern("M M")
            .pattern("M M")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WATER_ASPECTED_LEGGINGS.get())
            .pattern("WWW")
            .pattern("W W")
            .pattern("W W")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WOOD_ASPECTED_LEGGINGS.get())
            .pattern("WWW")
            .pattern("W W")
            .pattern("W W")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.SKY_IRON_LEGGINGS.get())
            .pattern("SSS")
            .pattern("S S")
            .pattern("S S")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildBootsRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.EARTH_ASPECTED_BOOTS.get())
            .pattern("E E")
            .pattern("E E")
            .define('E', ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ItemRegistry.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.FIRE_ASPECTED_BOOTS.get())
            .pattern("F F")
            .pattern("F F")
            .define('F', ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ItemRegistry.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.METAL_ASPECTED_BOOTS.get())
            .pattern("M M")
            .pattern("M M")
            .define('M', ItemRegistry.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ItemRegistry.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WATER_ASPECTED_BOOTS.get())
            .pattern("W W")
            .pattern("W W")
            .define('W', ItemRegistry.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ItemRegistry.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.WOOD_ASPECTED_BOOTS.get())
            .pattern("W W")
            .pattern("W W")
            .define('W', ItemRegistry.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ItemRegistry.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.SKY_IRON_BOOTS.get())
            .pattern("S S")
            .pattern("S S")
            .define('S', ItemRegistry.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ItemRegistry.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildPillFurnaceRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.PILL_FURNACE_BLOCK.get())
            .pattern("III")
            .pattern("ISI")
            .pattern("CCC")
            .define('I', Items.IRON_INGOT)
            .define('S', BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .define('C', Blocks.COPPER_BLOCK)
            .unlockedBy("has_copper_ingot", helper.has(Items.COPPER_INGOT))
            .save(recipeOutput);
    }
}