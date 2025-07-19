package net.hyper.immortalcultivation.datagen.recipes;

import net.hyper.immortalcultivation.datagen.recipes.helper.ICModPillFurnaceRecipeHelper;
import net.hyper.immortalcultivation.datagen.recipes.helper.ICModShapedRecipeHelper;
import net.hyper.immortalcultivation.datagen.recipes.helper.ICModShapelessRecipeHelper;
import net.hyper.immortalcultivation.datagen.recipes.helper.ICModSmeltingRecipeHelper;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider implements IConditionBuilder {

    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        RecipeHelper helper = new RecipeHelper(this);

        ICModShapedRecipeHelper.buildShapedRecipes(recipeOutput, helper);
        ICModShapelessRecipeHelper.buildShapelessRecipes(recipeOutput, helper);
        ICModSmeltingRecipeHelper.buildSmeltingRecipes(recipeOutput, helper);
        ICModPillFurnaceRecipeHelper.buildPillFurnaceRecipes(recipeOutput, helper);
    }

    // Wrapper class to expose protected methods
    public static class RecipeHelper {
        private final net.minecraft.data.recipes.RecipeProvider provider;

        public RecipeHelper(net.minecraft.data.recipes.RecipeProvider provider) {
            this.provider = provider;
        }

        public Criterion<?> has(ItemLike item) {
            return provider.has(item);
        }

        public net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance has(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
            return provider.has(tag).triggerInstance();
        }

        public String getHasName(net.minecraft.world.level.ItemLike item) {
            return net.minecraft.data.recipes.RecipeProvider.getHasName(item);
        }

        public String getItemName(net.minecraft.world.level.ItemLike item) {
            return net.minecraft.data.recipes.RecipeProvider.getItemName(item);
        }
    }
}