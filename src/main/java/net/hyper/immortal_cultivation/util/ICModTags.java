package net.hyper.immortal_cultivation.util;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ICModTags {

    public static class Items {
        public static final TagKey<Item> SPIRIT_STONES = createTag("spirit_stones");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
        }
    }
}
