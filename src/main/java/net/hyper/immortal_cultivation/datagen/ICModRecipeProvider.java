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

    }
}
