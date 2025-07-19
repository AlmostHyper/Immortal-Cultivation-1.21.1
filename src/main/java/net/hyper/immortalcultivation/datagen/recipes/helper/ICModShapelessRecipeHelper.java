package net.hyper.immortalcultivation.datagen.recipes.helper;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.datagen.recipes.RecipeProvider;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;

public class ICModShapelessRecipeHelper {

    public static void buildShapelessRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {
        buildSpiritStoneRecipes(recipeOutput, helper);
    }

    private static void buildSpiritStoneRecipes(RecipeOutput recipeOutput, RecipeProvider.RecipeHelper helper) {

        // === SPIRIT STONE BREAKDOWN RECIPES (Higher quality → Lower quality) === //

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get(), 2)
            .requires(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE)
            .unlockedBy("has_medium_quality_spirit_stone", helper.has(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.LOW_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get(), 2)
            .requires(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE)
            .unlockedBy("has_high_quality_spirit_stone", helper.has(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get(), 2)
            .requires(ItemRegistry.TOP_QUALITY_SPIRIT_STONE)
            .unlockedBy("has_top_quality_spirit_stone", helper.has(ItemRegistry.TOP_QUALITY_SPIRIT_STONE))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(ItemRegistry.TOP_QUALITY_SPIRIT_STONE));

        // === SPIRIT STONE UNPACKING RECIPES (Block → Items) === //

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_low_quality_spirit_stone_block", helper.has(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.LOW_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_medium_quality_spirit_stone_block", helper.has(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_high_quality_spirit_stone_block", helper.has(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_top_quality_spirit_stone_block", helper.has(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK))
            .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(ItemRegistry.TOP_QUALITY_SPIRIT_STONE) + "_from_" + helper.getItemName(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK));
    }
}