package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModItems;
import net.hyper.immortalcultivation.util.ICModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ICModItemTagProvider extends ItemTagsProvider {
    public ICModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
                                super(output, lookupProvider, blockTags, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ICModTags.Items.SPIRIT_STONES)
            .add(ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
            .add(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .add(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
            .add(ICModItems.TOP_QUALITY_SPIRIT_STONE.get());

        tag(Tags.Items.ORES)
            .add(ICModItems.EARTH_ASPECTED_ORE.get())
            .add(ICModItems.FIRE_ASPECTED_ORE.get())
            .add(ICModItems.METAL_ASPECTED_ORE.get())
            .add(ICModItems.SKY_IRON_ORE.get())
            .add(ICModItems.WATER_ASPECTED_ORE.get())
            .add(ICModItems.WOOD_ASPECTED_ORE.get());

        tag(Tags.Items.INGOTS)
            .add(ICModItems.EARTH_ASPECTED_INGOT.get())
            .add(ICModItems.FIRE_ASPECTED_INGOT.get())
            .add(ICModItems.METAL_ASPECTED_INGOT.get())
            .add(ICModItems.SKY_IRON_INGOT.get())
            .add(ICModItems.WATER_ASPECTED_INGOT.get())
            .add(ICModItems.WOOD_ASPECTED_INGOT.get());

        tag(ItemTags.SWORDS)
            .add(ICModItems.EARTH_ASPECTED_SWORD.get())
            .add(ICModItems.FIRE_ASPECTED_SWORD.get())
            .add(ICModItems.METAL_ASPECTED_SWORD.get())
            .add(ICModItems.WATER_ASPECTED_SWORD.get())
            .add(ICModItems.WOOD_ASPECTED_SWORD.get())
            .add(ICModItems.SKY_IRON_SWORD.get());

        tag(ItemTags.PICKAXES)
            .add(ICModItems.EARTH_ASPECTED_PICKAXE.get())
            .add(ICModItems.FIRE_ASPECTED_PICKAXE.get())
            .add(ICModItems.METAL_ASPECTED_PICKAXE.get())
            .add(ICModItems.WATER_ASPECTED_PICKAXE.get())
            .add(ICModItems.WOOD_ASPECTED_PICKAXE.get())
            .add(ICModItems.SKY_IRON_PICKAXE.get());

        tag(ItemTags.AXES)
            .add(ICModItems.EARTH_ASPECTED_AXE.get())
            .add(ICModItems.FIRE_ASPECTED_AXE.get())
            .add(ICModItems.METAL_ASPECTED_AXE.get())
            .add(ICModItems.WATER_ASPECTED_AXE.get())
            .add(ICModItems.WOOD_ASPECTED_AXE.get())
            .add(ICModItems.SKY_IRON_AXE.get());

        tag(ItemTags.SHOVELS)
            .add(ICModItems.EARTH_ASPECTED_SHOVEL.get())
            .add(ICModItems.FIRE_ASPECTED_SHOVEL.get())
            .add(ICModItems.METAL_ASPECTED_SHOVEL.get())
            .add(ICModItems.WATER_ASPECTED_SHOVEL.get())
            .add(ICModItems.WOOD_ASPECTED_SHOVEL.get())
            .add(ICModItems.SKY_IRON_SHOVEL.get());

        tag(ItemTags.HOES)
            .add(ICModItems.EARTH_ASPECTED_HOE.get())
            .add(ICModItems.FIRE_ASPECTED_HOE.get())
            .add(ICModItems.METAL_ASPECTED_HOE.get())
            .add(ICModItems.WATER_ASPECTED_HOE.get())
            .add(ICModItems.WOOD_ASPECTED_HOE.get())
            .add(ICModItems.SKY_IRON_HOE.get());

        tag(ItemTags.HEAD_ARMOR)
            .add(ICModItems.EARTH_ASPECTED_HELMET.get())
            .add(ICModItems.FIRE_ASPECTED_HELMET.get())
            .add(ICModItems.METAL_ASPECTED_HELMET.get())
            .add(ICModItems.WATER_ASPECTED_HELMET.get())
            .add(ICModItems.WOOD_ASPECTED_HELMET.get())
            .add(ICModItems.SKY_IRON_HELMET.get());

        tag(ItemTags.CHEST_ARMOR)
            .add(ICModItems.EARTH_ASPECTED_CHESTPLATE.get())
            .add(ICModItems.FIRE_ASPECTED_CHESTPLATE.get())
            .add(ICModItems.METAL_ASPECTED_CHESTPLATE.get())
            .add(ICModItems.WATER_ASPECTED_CHESTPLATE.get())
            .add(ICModItems.WOOD_ASPECTED_CHESTPLATE.get())
            .add(ICModItems.SKY_IRON_CHESTPLATE.get());

        tag(ItemTags.LEG_ARMOR)
            .add(ICModItems.EARTH_ASPECTED_LEGGINGS.get())
            .add(ICModItems.FIRE_ASPECTED_LEGGINGS.get())
            .add(ICModItems.METAL_ASPECTED_LEGGINGS.get())
            .add(ICModItems.WATER_ASPECTED_LEGGINGS.get())
            .add(ICModItems.WOOD_ASPECTED_LEGGINGS.get())
            .add(ICModItems.SKY_IRON_LEGGINGS.get());

        tag(ItemTags.FOOT_ARMOR)
            .add(ICModItems.EARTH_ASPECTED_BOOTS.get())
            .add(ICModItems.FIRE_ASPECTED_BOOTS.get())
            .add(ICModItems.METAL_ASPECTED_BOOTS.get())
            .add(ICModItems.WATER_ASPECTED_BOOTS.get())
            .add(ICModItems.WOOD_ASPECTED_BOOTS.get())
            .add(ICModItems.SKY_IRON_BOOTS.get());
    }
}
