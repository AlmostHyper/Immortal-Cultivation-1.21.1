package net.hyper.immortalcultivation.data.recipes;

import net.hyper.immortalcultivation.data.recipes.helper.ICModPillFurnaceRecipeHelper;
import net.hyper.immortalcultivation.data.recipes.helper.ICModShapedRecipeHelper;
import net.hyper.immortalcultivation.data.recipes.helper.ICModShapelessRecipeHelper;
import net.hyper.immortalcultivation.data.recipes.helper.ICModSmeltingRecipeHelper;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ICModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ICModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
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
        private final RecipeProvider provider;

        public RecipeHelper(RecipeProvider provider) {
            this.provider = provider;
        }

        public Criterion<?> has(ItemLike item) {
            return provider.has(item);
        }

        public net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance has(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
            return provider.has(tag).triggerInstance();
        }

        public String getHasName(net.minecraft.world.level.ItemLike item) {
            return RecipeProvider.getHasName(item);
        }

        public String getItemName(net.minecraft.world.level.ItemLike item) {
            return RecipeProvider.getItemName(item);
        }
    }
}