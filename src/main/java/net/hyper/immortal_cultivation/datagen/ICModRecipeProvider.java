package net.hyper.immortal_cultivation.datagen;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.hyper.immortal_cultivation.registry.ICModBlocks;
import net.hyper.immortal_cultivation.registry.ICModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ICModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ICModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        // Crafting

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
                .pattern("LLL")
                .pattern("LLL")
                .pattern("LLL")
                .define('L', ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
                .unlockedBy("has_low_quality_spirit_stone", has(ICModItems.LOW_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
                .pattern("LLL")
                .pattern("LLL")
                .pattern("LLL")
                .define('L', ICModItems.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
                .unlockedBy("has_low_quality_spirit_stone_block", has(ICModItems.LOW_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.LOW_QUALITY_SPIRIT_STONE.get(), 9)
                .requires(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK)
                .unlockedBy("has_low_quality_spirit_stone_block", has(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
                .pattern("MMM")
                .pattern("MMM")
                .pattern("MMM")
                .define('M', ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
                .unlockedBy("has_medium_quality_spirit_stone", has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
                .pattern("MMM")
                .pattern("MMM")
                .pattern("MMM")
                .define('M', ICModItems.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
                .unlockedBy("has_medium_quality_spirit_stone_block", has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get(), 9)
                .requires(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)
                .unlockedBy("has_medium_quality_spirit_stone_block", has(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
                .pattern("HHH")
                .pattern("HHH")
                .pattern("HHH")
                .define('H', ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
                .unlockedBy("has_high_quality_spirit_stone", has(ICModItems.HIGH_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
                .pattern("HHH")
                .pattern("HHH")
                .pattern("HHH")
                .define('H', ICModItems.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
                .unlockedBy("has_high_quality_spirit_stone_block", has(ICModItems.HIGH_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.HIGH_QUALITY_SPIRIT_STONE.get(), 9)
                .requires(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK)
                .unlockedBy("has_high_quality_spirit_stone_block", has(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
                .pattern("TTT")
                .pattern("TTT")
                .pattern("TTT")
                .define('T', ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
                .unlockedBy("has_top_quality_spirit_stone", has(ICModItems.TOP_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
                .pattern("TTT")
                .pattern("TTT")
                .pattern("TTT")
                .define('T', ICModItems.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
                .unlockedBy("has_top_quality_spirit_stone_block", has(ICModItems.TOP_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.TOP_QUALITY_SPIRIT_STONE.get(), 9)
                .requires(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK)
                .unlockedBy("has_top_quality_spirit_stone_block", has(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        // Smelting

        List<ItemLike> EARTH_ASPECTED_SMELTABLES = List.of(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK, ICModItems.EARTH_ASPECTED_ORE);
        List<ItemLike> FIRE_ASPECTED_SMELTABLES = List.of(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK, ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.FIRE_ASPECTED_ORE);
        List<ItemLike> METAL_ASPECTED_SMELTABLES = List.of(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.METAL_ASPECTED_ORE);
        List<ItemLike> SKY_IRON_SMELTABLES = List.of(ICModBlocks.SKY_IRON_ORE_BLOCK, ICModItems.SKY_IRON_ORE);
        List<ItemLike> WATER_ASPECTED_SMELTABLES = List.of(ICModBlocks.WATER_ASPECTED_ORE_BLOCK, ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.WATER_ASPECTED_ORE);
        List<ItemLike> WOOD_ASPECTED_SMELTABLES = List.of(ICModItems.WOOD_ASPECTED_ORE);

        oreSmelting(recipeOutput, EARTH_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_INGOT.get(), 0.25f, 200, "earth_aspected_ingot");
        oreBlasting(recipeOutput, EARTH_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_INGOT.get(), 0.25f, 100, "earth_aspected_ingot");

        oreSmelting(recipeOutput, FIRE_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_INGOT.get(), 0.25f, 200, "fire_aspected_ingot");
        oreBlasting(recipeOutput, FIRE_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_INGOT.get(), 0.25f, 100, "fire_aspected_ingot");

        oreSmelting(recipeOutput, METAL_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.METAL_ASPECTED_INGOT.get(), 0.25f, 200, "metal_aspected_ingot");
        oreBlasting(recipeOutput, METAL_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.METAL_ASPECTED_INGOT.get(), 0.25f, 100, "metal_aspected_ingot");

        oreSmelting(recipeOutput, SKY_IRON_SMELTABLES, RecipeCategory.MISC, ICModItems.SKY_IRON_INGOT.get(), 0.5f, 400, "sky_iron_ingot");
        oreBlasting(recipeOutput, SKY_IRON_SMELTABLES, RecipeCategory.MISC, ICModItems.SKY_IRON_INGOT.get(), 0.5f, 200, "sky_iron_ingot");

        oreSmelting(recipeOutput, WATER_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WATER_ASPECTED_INGOT.get(), 0.25f, 200, "water_aspected_ingot");
        oreBlasting(recipeOutput, WATER_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WATER_ASPECTED_INGOT.get(), 0.25f, 100, "water_aspected_ingot");

        oreSmelting(recipeOutput, WOOD_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_INGOT.get(), 0.25f, 200, "wood_aspected_ingot");
        oreBlasting(recipeOutput, WOOD_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_INGOT.get(), 0.25f, 100, "wood_aspected_ingot");
    }

    // Helper Methods

    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
