package net.hyper.immortal_cultivation.item.tools;

import net.hyper.immortal_cultivation.registry.ICModItems;
import net.hyper.immortal_cultivation.util.ICModTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ICModToolTiers {
    public static final Tier EARTH_ASPECTED = new SimpleTier(ICModTags.Blocks.NEEDS_EARTH_ASPECTED_TOOL,
            1900, 4f, 3f, 19, () -> Ingredient.of(ICModItems.EARTH_ASPECTED_INGOT));

    public static final Tier FIRE_ASPECTED = new SimpleTier(ICModTags.Blocks.NEEDS_FIRE_ASPECTED_TOOL,
            1900, 4f, 3f, 19, () -> Ingredient.of(ICModItems.FIRE_ASPECTED_INGOT));

    public static final Tier METAL_ASPECTED = new SimpleTier(ICModTags.Blocks.NEEDS_METAL_ASPECTED_TOOL,
            1900, 4f, 3f, 19, () -> Ingredient.of(ICModItems.METAL_ASPECTED_INGOT));

    public static final Tier WATER_ASPECTED = new SimpleTier(ICModTags.Blocks.NEEDS_WATER_ASPECTED_TOOL,
            1900, 4f, 3f, 19, () -> Ingredient.of(ICModItems.WATER_ASPECTED_INGOT));

    public static final Tier WOOD_ASPECTED = new SimpleTier(ICModTags.Blocks.NEEDS_WOOD_ASPECTED_TOOL,
            1900, 4f, 3f, 19, () -> Ingredient.of(ICModItems.WOOD_ASPECTED_INGOT));

    public static final Tier SKY_IRON = new SimpleTier(ICModTags.Blocks.NEEDS_SKY_IRON_TOOL,
        3247, 4f, 3f, 28, () -> Ingredient.of(ICModItems.SKY_IRON_INGOT));
}
