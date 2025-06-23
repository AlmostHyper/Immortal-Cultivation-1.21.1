package net.hyper.immortalcultivation.builder;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.recipe.PillFurnaceRecipe;
import net.hyper.immortalcultivation.registry.ICModRecipes;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class PillFurnaceRecipeBuilder implements RecipeBuilder {
    private final ItemStack result;
    private Ingredient input1 = Ingredient.EMPTY;
    private Ingredient input2 = Ingredient.EMPTY;
    private Ingredient input3 = Ingredient.EMPTY;
    private int input1Count = 1;
    private int input2Count = 1;
    private int input3Count = 1;
    private boolean requiresSpiritStone = true;
    private double baseSuccessChance = 0.8;

    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    private PillFurnaceRecipeBuilder(ItemStack result) {
        this.result = result;
    }

    public static PillFurnaceRecipeBuilder pillRecipe(ItemLike result) {
        return pillRecipe(new ItemStack(result));
    }

    public static PillFurnaceRecipeBuilder pillRecipe(ItemLike result, int count) {
        return pillRecipe(new ItemStack(result, count));
    }

    public static PillFurnaceRecipeBuilder pillRecipe(ItemStack result) {
        return new PillFurnaceRecipeBuilder(result);
    }

    public PillFurnaceRecipeBuilder input1(ItemLike item) {
        return input1(Ingredient.of(item));
    }

    public PillFurnaceRecipeBuilder input1(ItemLike item, int count) {
        this.input1Count = count;
        return input1(Ingredient.of(item));
    }

    public PillFurnaceRecipeBuilder input1(Ingredient ingredient) {
        this.input1 = ingredient;
        return this;
    }

    public PillFurnaceRecipeBuilder input1(Ingredient ingredient, int count) {
        this.input1 = ingredient;
        this.input1Count = count;
        return this;
    }

    public PillFurnaceRecipeBuilder input2(ItemLike item) {
        return input2(Ingredient.of(item));
    }

    public PillFurnaceRecipeBuilder input2(ItemLike item, int count) {
        this.input2Count = count;
        return input2(Ingredient.of(item));
    }

    public PillFurnaceRecipeBuilder input2(Ingredient ingredient) {
        this.input2 = ingredient;
        return this;
    }

    public PillFurnaceRecipeBuilder input2(Ingredient ingredient, int count) {
        this.input2 = ingredient;
        this.input2Count = count;
        return this;
    }

    public PillFurnaceRecipeBuilder input3(ItemLike item) {
        return input3(Ingredient.of(item));
    }

    public PillFurnaceRecipeBuilder input3(ItemLike item, int count) {
        this.input3Count = count;
        return input3(Ingredient.of(item));
    }

    public PillFurnaceRecipeBuilder input3(Ingredient ingredient) {
        this.input3 = ingredient;
        return this;
    }

    public PillFurnaceRecipeBuilder input3(Ingredient ingredient, int count) {
        this.input3 = ingredient;
        this.input3Count = count;
        return this;
    }

    public PillFurnaceRecipeBuilder requiresSpiritStone(boolean requires) {
        this.requiresSpiritStone = requires;
        return this;
    }

    public PillFurnaceRecipeBuilder successChance(double chance) {
        this.baseSuccessChance = Math.max(0.0, Math.min(1.0, chance));
        return this;
    }

    @Override
    public PillFurnaceRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public PillFurnaceRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        this.ensureValid(id);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);

        this.criteria.forEach(advancement::addCriterion);

        PillFurnaceRecipe recipe = new PillFurnaceRecipe(
            this.input1,
            this.input2,
            this.input3,
            this.result,
            this.input1Count,
            this.input2Count,
            this.input3Count,
            this.requiresSpiritStone,
            this.baseSuccessChance
        );

        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/pill_furnace/")));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
        if (this.input1.isEmpty()) {
            throw new IllegalStateException("Pill recipe " + id + " must have at least input1 defined");
        }
    }

    // Convenience method to save with automatic naming
    public void save(RecipeOutput output, String name) {
        this.save(output, ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
    }

    // Convenience method to save with automatic naming based on result item
    public void save(RecipeOutput output) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID,
            getItemName(this.result.getItem()));
        this.save(output, itemId);
    }

    private static String getItemName(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            throw new IllegalStateException("Item " + item + " does not have a registry name");
        }
        return key.getPath();
    }
}