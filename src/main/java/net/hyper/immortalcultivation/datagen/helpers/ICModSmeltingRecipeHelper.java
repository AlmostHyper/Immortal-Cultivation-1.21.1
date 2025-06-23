package net.hyper.immortalcultivation.datagen.helpers;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.hyper.immortalcultivation.registry.ICModItems;
import net.hyper.immortalcultivation.datagen.ICModRecipeProvider;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public class ICModSmeltingRecipeHelper {

    public static void buildSmeltingRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        buildSmeltingAndBlastingRecipes(recipeOutput, helper);
    }

    private static void buildSmeltingAndBlastingRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {

        // === MORTAL REALM === //

        List<ItemLike> EARTH_ASPECTED_SMELTABLES = List.of(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK, ICModItems.EARTH_ASPECTED_ORE);
        List<ItemLike> FIRE_ASPECTED_SMELTABLES = List.of(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK, ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.FIRE_ASPECTED_ORE);
        List<ItemLike> METAL_ASPECTED_SMELTABLES = List.of(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.METAL_ASPECTED_ORE);
        List<ItemLike> WATER_ASPECTED_SMELTABLES = List.of(ICModBlocks.WATER_ASPECTED_ORE_BLOCK, ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.WATER_ASPECTED_ORE);
        List<ItemLike> WOOD_ASPECTED_SMELTABLES = List.of(ICModItems.WOOD_ASPECTED_ORE);
        List<ItemLike> SKY_IRON_SMELTABLES = List.of(ICModBlocks.SKY_IRON_ORE_BLOCK, ICModItems.SKY_IRON_ORE);

        oreSmelting(recipeOutput, EARTH_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_INGOT.get(), 0.25f, 200, "earth_aspected_ingot", helper);
        oreBlasting(recipeOutput, EARTH_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_INGOT.get(), 0.25f, 100, "earth_aspected_ingot", helper);

        oreSmelting(recipeOutput, FIRE_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_INGOT.get(), 0.25f, 200, "fire_aspected_ingot", helper);
        oreBlasting(recipeOutput, FIRE_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_INGOT.get(), 0.25f, 100, "fire_aspected_ingot", helper);

        oreSmelting(recipeOutput, METAL_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.METAL_ASPECTED_INGOT.get(), 0.25f, 200, "metal_aspected_ingot", helper);
        oreBlasting(recipeOutput, METAL_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.METAL_ASPECTED_INGOT.get(), 0.25f, 100, "metal_aspected_ingot", helper);

        oreSmelting(recipeOutput, WATER_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WATER_ASPECTED_INGOT.get(), 0.25f, 200, "water_aspected_ingot", helper);
        oreBlasting(recipeOutput, WATER_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WATER_ASPECTED_INGOT.get(), 0.25f, 100, "water_aspected_ingot", helper);

        oreSmelting(recipeOutput, WOOD_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_INGOT.get(), 0.25f, 200, "wood_aspected_ingot", helper);
        oreBlasting(recipeOutput, WOOD_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_INGOT.get(), 0.25f, 100, "wood_aspected_ingot", helper);

        oreSmelting(recipeOutput, SKY_IRON_SMELTABLES, RecipeCategory.MISC, ICModItems.SKY_IRON_INGOT.get(), 0.5f, 400, "sky_iron_ingot", helper);
        oreBlasting(recipeOutput, SKY_IRON_SMELTABLES, RecipeCategory.MISC, ICModItems.SKY_IRON_INGOT.get(), 0.5f, 200, "sky_iron_ingot", helper);
    }

    private static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                    float pExperience, int pCookingTime, String pGroup, ICModRecipeProvider.RecipeHelper helper) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
            pExperience, pCookingTime, pGroup, "_from_smelting", helper);
    }

    private static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                    float pExperience, int pCookingTime, String pGroup, ICModRecipeProvider.RecipeHelper helper) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
            pExperience, pCookingTime, pGroup, "_from_blasting", helper);
    }

    private static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> pCookingSerializer,
                                                                     AbstractCookingRecipe.Factory<T> factory, List<ItemLike> pIngredients,
                                                                     RecipeCategory pCategory, ItemLike pResult, float pExperience,
                                                                     int pCookingTime, String pGroup, String pRecipeName, ICModRecipeProvider.RecipeHelper helper) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory)
                .group(pGroup)
                .unlockedBy(helper.getHasName(itemlike), helper.has(itemlike))
                .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + helper.getItemName(pResult) + pRecipeName + "_" + helper.getItemName(itemlike));
        }
    }
}