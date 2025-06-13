package net.hyper.immortalcultivation.util;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ICModTags {

    public static class Blocks {
        public static final TagKey<Block> NEEDS_EARTH_ASPECTED_TOOL = createTag("needs_earth_aspected_tool");
        public static final TagKey<Block> INCORRECT_FOR_EARTH_ASPECTED_TOOL = createTag("incorrect_for_earth_aspected_tool");

        public static final TagKey<Block> NEEDS_FIRE_ASPECTED_TOOL = createTag("needs_fire_aspected_tool");
        public static final TagKey<Block> INCORRECT_FOR_FIRE_ASPECTED_TOOL = createTag("incorrect_for_fire_aspected_tool");

        public static final TagKey<Block> NEEDS_METAL_ASPECTED_TOOL = createTag("needs_metal_aspected_tool");
        public static final TagKey<Block> INCORRECT_FOR_METAL_ASPECTED_TOOL = createTag("incorrect_for_metal_aspected_tool");

        public static final TagKey<Block> NEEDS_SKY_IRON_TOOL = createTag("needs_sky_iron_tool");
        public static final TagKey<Block> INCORRECT_FOR_SKY_IRON_TOOL = createTag("incorrect_for_sky_iron_tool");

        public static final TagKey<Block> NEEDS_WATER_ASPECTED_TOOL = createTag("needs_water_aspected_tool");
        public static final TagKey<Block> INCORRECT_FOR_WATER_ASPECTED_TOOL = createTag("incorrect_for_water_aspected_tool");

        public static final TagKey<Block> NEEDS_WOOD_ASPECTED_TOOL = createTag("needs_wood_aspected_tool");
        public static final TagKey<Block> INCORRECT_FOR_WOOD_ASPECTED_TOOL = createTag("incorrect_for_wood_aspected_tool");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));

        }
    }

    public static class Items {
        public static final TagKey<Item> SPIRIT_STONES = createTag("spirit_stones");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
        }
    }
}
