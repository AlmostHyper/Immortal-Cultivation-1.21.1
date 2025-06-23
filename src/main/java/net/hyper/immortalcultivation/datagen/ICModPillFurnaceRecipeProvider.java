package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.builder.PillFurnaceRecipeBuilder;
import net.hyper.immortalcultivation.registry.ICModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ICModPillFurnaceRecipeProvider extends RecipeProvider {

    public ICModPillFurnaceRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {

        // === MORTAL REALM === //
        /*
        // Qi Absorption Pills
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.QI_GATHERING_PILL.get())
            .input1(ICModItems.SPIRIT_HERB.get(), 2)
            .input2(ICModItems.MOON_LOTUS_PETAL.get())
            .input3(Items.GLOWSTONE_DUST)
            .requiresSpiritStone(true)
            .successChance(0.7)
            .unlockedBy("has_spirit_herb", has(ICModItems.SPIRIT_HERB.get()))
            .save(output, "qi_gathering_pill");


        // Healing Pill - simpler recipe with only one input
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.HEALING_PILL.get(), 2)
            .input1(ICModItems.BLOOD_GINSENG.get(), 3)
            .requiresSpiritStone(false)
            .successChance(0.9)
            .unlockedBy("has_blood_ginseng", has(ICModItems.BLOOD_GINSENG.get()))
            .save(output, "healing_pill");

        // Foundation Building Pill - complex recipe with low success rate
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.FOUNDATION_BUILDING_PILL.get())
            .input1(ICModItems.THOUSAND_YEAR_GINSENG.get())
            .input2(ICModItems.PHOENIX_FEATHER.get())
            .input3(ICModItems.DRAGON_BLOOD.get())
            .requiresSpiritStone(true)
            .successChance(0.3)
            .unlockedBy("has_phoenix_feather", has(ICModItems.PHOENIX_FEATHER.get()))
            .save(output, "foundation_building_pill");

        // Example with ingredient tags
        PillFurnaceRecipeBuilder.pillRecipe(ICModItems.SPIRIT_RESTORATION_PILL.get())
            .input1(Ingredient.of(ICModTags.Items.SPIRIT_HERBS), 4)
            .input2(ICModItems.CRYSTAL_DEW.get(), 2)
            .requiresSpiritStone(true)
            .successChance(0.6)
            .unlockedBy("has_spirit_herbs", has(ICModTags.Items.SPIRIT_HERBS))
            .save(output); // This will auto-generate the name from the result item */


    }
}