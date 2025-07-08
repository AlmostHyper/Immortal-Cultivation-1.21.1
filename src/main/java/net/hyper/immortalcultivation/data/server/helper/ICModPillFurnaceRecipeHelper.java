package net.hyper.immortalcultivation.data.server.helper;

import net.hyper.immortalcultivation.content.recipe.builders.PillFurnaceRecipeBuilder;
import net.hyper.immortalcultivation.data.server.ICModRecipeProvider;
import net.hyper.immortalcultivation.common.registry.ICModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

public class ICModPillFurnaceRecipeHelper {

    public static void buildPillFurnaceRecipes(RecipeOutput recipeOutput, ICModRecipeProvider.RecipeHelper helper) {
        // === MORTAL REALM PILLS === //

        /* == QI CULTIVATION PILLS == */

        // Low Quality Qi Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.LOW_QUALITY_QI_CULTIVATION_PILL.get())
            .input1(ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
            .input2(Items.REDSTONE)
            .input3(Items.LAPIS_LAZULI)
            .requiresSpiritStone(false)
            .successChance(0.8)
            .unlockedBy("has_low_quality_spirit_stone", helper.has(ICModItems.LOW_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "low_quality_qi_absorption_pill");

        // Medium Quality Qi Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.MEDIUM_QUALITY_QI_CULTIVATION_PILL.get())
            .input1(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .input2(Items.REDSTONE)
            .input3(Items.QUARTZ)
            .requiresSpiritStone(false)
            .successChance(0.6)
            .unlockedBy("has_medium_quality_spirit_stone", helper.has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "medium_quality_qi_absorption_pill");

        // High Quality Qi Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.HIGH_QUALITY_QI_CULTIVATION_PILL.get())
            .input1(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
            .input2(Items.REDSTONE)
            .input3(Items.END_CRYSTAL)
            .requiresSpiritStone(false)
            .successChance(0.4)
            .unlockedBy("has_high_quality_spirit_stone", helper.has(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "high_quality_qi_absorption_pill");

        // Top Quality Qi Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.TOP_QUALITY_QI_CULTIVATION_PILL.get())
            .input1(ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .input2(ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .input3(ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .requiresSpiritStone(false)
            .successChance(0.2)
            .unlockedBy("has_top_quality_spirit_stone", helper.has(ICModItems.TOP_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "top_quality_qi_absorption_pill");

        /* == BODY CULTIVATION PILLS == */

        // Low Quality Body Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.LOW_QUALITY_BODY_CULTIVATION_PILL.get())
            .input1(ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
            .input2(Items.REDSTONE)
            .input3(Items.IRON_INGOT)
            .requiresSpiritStone(false)
            .successChance(0.8)
            .unlockedBy("has_low_quality_spirit_stone", helper.has(ICModItems.LOW_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "low_quality_body_absorption_pill");

        // Medium Quality Body Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.MEDIUM_QUALITY_BODY_CULTIVATION_PILL.get())
            .input1(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .input2(Items.REDSTONE)
            .input3(Items.NETHER_BRICK)
            .requiresSpiritStone(false)
            .successChance(0.6)
            .unlockedBy("has_medium_quality_spirit_stone", helper.has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "medium_quality_body_absorption_pill");

        // High Quality Body Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.HIGH_QUALITY_BODY_CULTIVATION_PILL.get())
            .input1(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
            .input2(Items.REDSTONE)
            .input3(Items.NETHERITE_INGOT)
            .requiresSpiritStone(false)
            .successChance(0.4)
            .unlockedBy("has_high_quality_spirit_stone", helper.has(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "high_quality_body_absorption_pill");

        // Top Quality Body Cultivation Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.TOP_QUALITY_BODY_CULTIVATION_PILL.get())
            .input1(ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .input2(ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .input3(ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .requiresSpiritStone(false)
            .successChance(0.2)
            .unlockedBy("has_top_quality_spirit_stone", helper.has(ICModItems.TOP_QUALITY_SPIRIT_STONE.get()))
            .save(recipeOutput, "top_quality_body_absorption_pill");



        /*
        // Healing Pill - simpler recipe with only one input
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.HEALING_PILL.get(), 2)
            .input1(ICModItems.BLOOD_GINSENG.get(), 3)
            .requiresSpiritStone(false)
            .successChance(0.9)
            .unlockedBy("has_blood_ginseng", helper.has(ICModItems.BLOOD_GINSENG.get()))
            .save(recipeOutput, "healing_pill");

        // Foundation Building Pill - complex recipe with low success rate
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.FOUNDATION_BUILDING_PILL.get())
            .input1(ICModItems.THOUSAND_YEAR_GINSENG.get())
            .input2(ICModItems.PHOENIX_FEATHER.get())
            .input3(ICModItems.DRAGON_BLOOD.get())
            .requiresSpiritStone(true)
            .successChance(0.3)
            .unlockedBy("has_phoenix_feather", helper.has(ICModItems.PHOENIX_FEATHER.get()))
            .save(recipeOutput, "foundation_building_pill");

        // Example with ingredient tags
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.SPIRIT_RESTORATION_PILL.get())
            .input1(Ingredient.of(ICModTags.Items.SPIRIT_HERBS), 4)
            .input2(ICModItems.CRYSTAL_DEW.get(), 2)
            .requiresSpiritStone(true)
            .successChance(0.6)
            .unlockedBy("has_spirit_herbs", helper.has(ICModTags.Items.SPIRIT_HERBS))
            .save(recipeOutput); // This will auto-generate the name from the result item
        */
    }
}