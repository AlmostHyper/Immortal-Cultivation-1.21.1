package net.hyper.immortalcultivation.content.item.tools;

import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.hyper.immortalcultivation.datagen.Tags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ICModToolTiers {
    public static final Tier EARTH_ASPECTED = new SimpleTier(Tags.Blocks.NEEDS_EARTH_ASPECTED_TOOL,
            999, 4f, 2.5f, 19, () -> Ingredient.of(ItemRegistry.EARTH_ASPECTED_INGOT));

    public static final Tier FIRE_ASPECTED = new SimpleTier(Tags.Blocks.NEEDS_FIRE_ASPECTED_TOOL,
            999, 4f, 2.5f, 19, () -> Ingredient.of(ItemRegistry.FIRE_ASPECTED_INGOT));

    public static final Tier METAL_ASPECTED = new SimpleTier(Tags.Blocks.NEEDS_METAL_ASPECTED_TOOL,
            999, 4f, 2.5f, 19, () -> Ingredient.of(ItemRegistry.METAL_ASPECTED_INGOT));

    public static final Tier WATER_ASPECTED = new SimpleTier(Tags.Blocks.NEEDS_WATER_ASPECTED_TOOL,
            999, 4f, 2.5f, 19, () -> Ingredient.of(ItemRegistry.WATER_ASPECTED_INGOT));

    public static final Tier WOOD_ASPECTED = new SimpleTier(Tags.Blocks.NEEDS_WOOD_ASPECTED_TOOL,
            999, 4f, 2.5f, 19, () -> Ingredient.of(ItemRegistry.WOOD_ASPECTED_INGOT));

    public static final Tier SKY_IRON = new SimpleTier(Tags.Blocks.NEEDS_SKY_IRON_TOOL,
        3247, 4f, 4.5f, 28, () -> Ingredient.of(ItemRegistry.SKY_IRON_INGOT));
}
