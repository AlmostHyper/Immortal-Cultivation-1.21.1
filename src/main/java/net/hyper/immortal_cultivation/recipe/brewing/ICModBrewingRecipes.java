package net.hyper.immortal_cultivation.recipe.brewing;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.hyper.immortal_cultivation.registry.ICModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

@EventBusSubscriber
public class ICModBrewingRecipes implements IBrewingRecipe {
    @SubscribeEvent
    public static void init(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(new ICModBrewingRecipes());
    }

    public boolean isInput(ItemStack input) {
        return Ingredient.of(new ItemStack[]{new ItemStack((ItemLike) ICModItems.LOW_QUALITY_SPIRIT_STONE)}).test(input);
    }

    public boolean isIngredient(ItemStack ingredient) {
        return Ingredient.of(new ItemStack[]{new ItemStack((ItemLike) Items.GLOW_BERRIES)}).test(ingredient);
    }

    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return this.isInput(input) && this.isIngredient(ingredient) ? new ItemStack((ItemLike) ICModItems.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get()) : ItemStack.EMPTY;
    }
}