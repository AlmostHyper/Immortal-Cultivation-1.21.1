package net.hyper.immortalcultivation.datagen.helpers;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.datagen.ICModRecipeProvider;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.hyper.immortalcultivation.registry.ICModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;

public class ICModShapelessRecipeHelper {

    public static void buildShapelessRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        buildSpiritStoneRecipes(recipeOutput, helper);
    }

    private static void buildSpiritStoneRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {

        // === SPIRIT STONE BREAKDOWN RECIPES (Higher quality → Lower quality) === //

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.LOW_QUALITY_SPIRIT_STONE.get(), 2)
            .requires(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE)
            .unlockedBy("has_medium_quality_spirit_stone", helper.has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.LOW_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get(), 2)
            .requires(ICModItems.HIGH_QUALITY_SPIRIT_STONE)
            .unlockedBy("has_high_quality_spirit_stone", helper.has(ICModItems.HIGH_QUALITY_SPIRIT_STONE))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModItems.HIGH_QUALITY_SPIRIT_STONE));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.HIGH_QUALITY_SPIRIT_STONE.get(), 2)
            .requires(ICModItems.TOP_QUALITY_SPIRIT_STONE)
            .unlockedBy("has_top_quality_spirit_stone", helper.has(ICModItems.TOP_QUALITY_SPIRIT_STONE))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.HIGH_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModItems.TOP_QUALITY_SPIRIT_STONE));

        // === SPIRIT STONE UNPACKING RECIPES (Block → Items) === //

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.LOW_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_low_quality_spirit_stone_block", helper.has(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.LOW_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_medium_quality_spirit_stone_block", helper.has(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.HIGH_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_high_quality_spirit_stone_block", helper.has(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.HIGH_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.TOP_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_top_quality_spirit_stone_block", helper.has(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ICModItems.TOP_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK));
    }
}