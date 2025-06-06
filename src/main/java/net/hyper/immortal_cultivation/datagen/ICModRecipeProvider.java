package net.hyper.immortal_cultivation.datagen;

import net.hyper.immortal_cultivation.registry.ICModBlocks;
import net.hyper.immortal_cultivation.registry.ICModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import java.util.concurrent.CompletableFuture;

public class ICModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ICModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
                .unlockedBy("has_low_quality_spirit_stone", has(ICModItems.LOW_QUALITY_SPIRIT_STONE)).save(recipeOutput);

    }
}
