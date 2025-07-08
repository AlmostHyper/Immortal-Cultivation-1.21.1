package net.hyper.immortalcultivation.data.server.helper;

import net.hyper.immortalcultivation.data.server.ICModRecipeProvider;
import net.hyper.immortalcultivation.common.registry.ICModBlocks;
import net.hyper.immortalcultivation.common.registry.ICModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ICModShapedRecipeHelper {

    public static void buildShapedRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        buildSpiritStoneRecipes(recipeOutput, helper);
        buildToolRecipes(recipeOutput, helper);
        buildArmorRecipes(recipeOutput, helper);
        buildPillFurnaceRecipes(recipeOutput, helper);
    }

    private static void buildSpiritStoneRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("LLL")
            .pattern("LLL")
            .pattern("LLL")
            .define('L', ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_low_quality_spirit_stone", helper.has(ICModItems.LOW_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("LLL")
            .pattern("LLL")
            .pattern("LLL")
            .define('L', ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_low_quality_spirit_stone_block", helper.has(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("MMM")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_medium_quality_spirit_stone", helper.has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("MMM")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_medium_quality_spirit_stone_block", helper.has(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("HHH")
            .pattern("HHH")
            .pattern("HHH")
            .define('H', ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_high_quality_spirit_stone", helper.has(ICModItems.HIGH_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("HHH")
            .pattern("HHH")
            .pattern("HHH")
            .define('H', ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_high_quality_spirit_stone_block", helper.has(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("TTT")
            .pattern("TTT")
            .pattern("TTT")
            .define('T', ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_top_quality_spirit_stone", helper.has(ICModItems.TOP_QUALITY_SPIRIT_STONE))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("TTT")
            .pattern("TTT")
            .pattern("TTT")
            .define('T', ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_top_quality_spirit_stone_block", helper.has(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput);
    }

    private static void buildToolRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        buildSwordRecipes(recipeOutput, helper);
        buildPickaxeRecipes(recipeOutput, helper);
        buildAxeRecipes(recipeOutput, helper);
        buildShovelRecipes(recipeOutput, helper);
        buildHoeRecipes(recipeOutput, helper);
    }

    private static void buildSwordRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.EARTH_ASPECTED_SWORD.get())
            .pattern("E")
            .pattern("E")
            .pattern("I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.FIRE_ASPECTED_SWORD.get())
            .pattern("F")
            .pattern("F")
            .pattern("I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.METAL_ASPECTED_SWORD.get())
            .pattern("M")
            .pattern("M")
            .pattern("I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.WATER_ASPECTED_SWORD.get())
            .pattern("W")
            .pattern("W")
            .pattern("I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.WOOD_ASPECTED_SWORD.get())
            .pattern("W")
            .pattern("W")
            .pattern("I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.SKY_IRON_SWORD.get())
            .pattern("S")
            .pattern("S")
            .pattern("I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildPickaxeRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_PICKAXE.get())
            .pattern("EEE")
            .pattern(" I ")
            .pattern(" I ")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_PICKAXE.get())
            .pattern("FFF")
            .pattern(" I ")
            .pattern(" I ")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_PICKAXE.get())
            .pattern("MMM")
            .pattern(" I ")
            .pattern(" I ")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_PICKAXE.get())
            .pattern("WWW")
            .pattern(" I ")
            .pattern(" I ")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_PICKAXE.get())
            .pattern("WWW")
            .pattern(" I ")
            .pattern(" I ")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_PICKAXE.get())
            .pattern("SSS")
            .pattern(" I ")
            .pattern(" I ")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildAxeRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_AXE.get())
            .pattern("EE")
            .pattern("EI")
            .pattern(" I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_AXE.get())
            .pattern("FF")
            .pattern("FI")
            .pattern(" I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_AXE.get())
            .pattern("MM")
            .pattern("MI")
            .pattern(" I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_AXE.get())
            .pattern("WW")
            .pattern("WI")
            .pattern(" I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_AXE.get())
            .pattern("WW")
            .pattern("WI")
            .pattern(" I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_AXE.get())
            .pattern("SS")
            .pattern("SI")
            .pattern(" I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildShovelRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_SHOVEL.get())
            .pattern("E")
            .pattern("I")
            .pattern("I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_SHOVEL.get())
            .pattern("F")
            .pattern("I")
            .pattern("I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_SHOVEL.get())
            .pattern("M")
            .pattern("I")
            .pattern("I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_SHOVEL.get())
            .pattern("W")
            .pattern("I")
            .pattern("I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_SHOVEL.get())
            .pattern("W")
            .pattern("I")
            .pattern("I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_SHOVEL.get())
            .pattern("S")
            .pattern("I")
            .pattern("I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildHoeRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_HOE.get())
            .pattern("EE")
            .pattern(" I")
            .pattern(" I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_HOE.get())
            .pattern("FF")
            .pattern(" I")
            .pattern(" I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_HOE.get())
            .pattern("MM")
            .pattern(" I")
            .pattern(" I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_HOE.get())
            .pattern("WW")
            .pattern(" I")
            .pattern(" I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_HOE.get())
            .pattern("WW")
            .pattern(" I")
            .pattern(" I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_HOE.get())
            .pattern("SS")
            .pattern(" I")
            .pattern(" I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildArmorRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        // === ARMOR === //
        buildHelmetRecipes(recipeOutput, helper);
        buildChestplateRecipes(recipeOutput, helper);
        buildLeggingsRecipes(recipeOutput, helper);
        buildBootsRecipes(recipeOutput, helper);
    }

    private static void buildHelmetRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_HELMET.get())
            .pattern("EEE")
            .pattern("E E")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_HELMET.get())
            .pattern("FFF")
            .pattern("F F")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_HELMET.get())
            .pattern("MMM")
            .pattern("M M")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_HELMET.get())
            .pattern("WWW")
            .pattern("W W")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_HELMET.get())
            .pattern("WWW")
            .pattern("W W")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_HELMET.get())
            .pattern("SSS")
            .pattern("S S")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildChestplateRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_CHESTPLATE.get())
            .pattern("E E")
            .pattern("EEE")
            .pattern("EEE")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_CHESTPLATE.get())
            .pattern("F F")
            .pattern("FFF")
            .pattern("FFF")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_CHESTPLATE.get())
            .pattern("M M")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_CHESTPLATE.get())
            .pattern("W W")
            .pattern("WWW")
            .pattern("WWW")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_CHESTPLATE.get())
            .pattern("W W")
            .pattern("WWW")
            .pattern("WWW")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_CHESTPLATE.get())
            .pattern("S S")
            .pattern("SSS")
            .pattern("SSS")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildLeggingsRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_LEGGINGS.get())
            .pattern("EEE")
            .pattern("E E")
            .pattern("E E")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_LEGGINGS.get())
            .pattern("FFF")
            .pattern("F F")
            .pattern("F F")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_LEGGINGS.get())
            .pattern("MMM")
            .pattern("M M")
            .pattern("M M")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_LEGGINGS.get())
            .pattern("WWW")
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_LEGGINGS.get())
            .pattern("WWW")
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_LEGGINGS.get())
            .pattern("SSS")
            .pattern("S S")
            .pattern("S S")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildBootsRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_BOOTS.get())
            .pattern("E E")
            .pattern("E E")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", helper.has(ICModItems.EARTH_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_BOOTS.get())
            .pattern("F F")
            .pattern("F F")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", helper.has(ICModItems.FIRE_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_BOOTS.get())
            .pattern("M M")
            .pattern("M M")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", helper.has(ICModItems.METAL_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_BOOTS.get())
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", helper.has(ICModItems.WATER_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_BOOTS.get())
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", helper.has(ICModItems.WOOD_ASPECTED_INGOT))
            .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_BOOTS.get())
            .pattern("S S")
            .pattern("S S")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", helper.has(ICModItems.SKY_IRON_INGOT))
            .save(recipeOutput);
    }

    private static void buildPillFurnaceRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.PILL_FURNACE_BLOCK.get())
            .pattern("III")
            .pattern("ISI")
            .pattern("CCC")
            .define('I', Items.IRON_INGOT)
            .define('S', ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .define('C', Blocks.COPPER_BLOCK)
            .unlockedBy("has_copper_ingot", helper.has(Items.COPPER_INGOT))
            .save(recipeOutput);
    }
}