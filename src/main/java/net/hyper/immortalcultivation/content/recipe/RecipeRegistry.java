package net.hyper.immortalcultivation.content.recipe;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.recipe.types.PillFurnaceRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, ImmortalCultivation.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, ImmortalCultivation.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PillFurnaceRecipe>> PILL_SERIALIZER =
        SERIALIZERS.register("forming", PillFurnaceRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<PillFurnaceRecipe>> PILL_FURNACE_TYPE =
        TYPES.register("forming", () -> new RecipeType<PillFurnaceRecipe>() {
            @Override
            public String toString() {
                return "forming";
            }
        });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}