package net.hyper.immortalcultivation.content.recipe.types;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Recipe input implementation for the Pill Furnace.
 * This class wraps the three input slots of the pill furnace
 * to work with Minecraft's recipe system.
 */
public record PillFurnaceRecipeInput(ItemStack item1, ItemStack item2, ItemStack item3) implements RecipeInput {

    /**
     * Gets the item at the specified index.
     * Index 0-2 correspond to the three input slots.
     *
     * @param index The slot index (0-2)
     * @return The ItemStack in that slot, or EMPTY if index is out of bounds
     */
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> item1;
            case 1 -> item2;
            case 2 -> item3;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return item1.isEmpty() && item2.isEmpty() && item3.isEmpty();
    }
}