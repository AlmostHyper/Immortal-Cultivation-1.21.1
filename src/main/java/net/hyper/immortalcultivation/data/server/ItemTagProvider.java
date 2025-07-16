package net.hyper.immortalcultivation.data.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.hyper.immortalcultivation.data.Tags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTagProvider extends ItemTagsProvider {
    public ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                           CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
                                super(output, lookupProvider, blockTags, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(Tags.Items.SPIRIT_STONES)
            .add(ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get())
            .add(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .add(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get())
            .add(ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get());

        tag(net.neoforged.neoforge.common.Tags.Items.ORES)
            .add(ItemRegistry.EARTH_ASPECTED_ORE.get())
            .add(ItemRegistry.FIRE_ASPECTED_ORE.get())
            .add(ItemRegistry.METAL_ASPECTED_ORE.get())
            .add(ItemRegistry.SKY_IRON_ORE.get())
            .add(ItemRegistry.WATER_ASPECTED_ORE.get())
            .add(ItemRegistry.WOOD_ASPECTED_ORE.get());

        tag(net.neoforged.neoforge.common.Tags.Items.INGOTS)
            .add(ItemRegistry.EARTH_ASPECTED_INGOT.get())
            .add(ItemRegistry.FIRE_ASPECTED_INGOT.get())
            .add(ItemRegistry.METAL_ASPECTED_INGOT.get())
            .add(ItemRegistry.SKY_IRON_INGOT.get())
            .add(ItemRegistry.WATER_ASPECTED_INGOT.get())
            .add(ItemRegistry.WOOD_ASPECTED_INGOT.get());

        tag(ItemTags.SWORDS)
            .add(ItemRegistry.EARTH_ASPECTED_SWORD.get())
            .add(ItemRegistry.FIRE_ASPECTED_SWORD.get())
            .add(ItemRegistry.METAL_ASPECTED_SWORD.get())
            .add(ItemRegistry.WATER_ASPECTED_SWORD.get())
            .add(ItemRegistry.WOOD_ASPECTED_SWORD.get())
            .add(ItemRegistry.SKY_IRON_SWORD.get());

        tag(ItemTags.PICKAXES)
            .add(ItemRegistry.EARTH_ASPECTED_PICKAXE.get())
            .add(ItemRegistry.FIRE_ASPECTED_PICKAXE.get())
            .add(ItemRegistry.METAL_ASPECTED_PICKAXE.get())
            .add(ItemRegistry.WATER_ASPECTED_PICKAXE.get())
            .add(ItemRegistry.WOOD_ASPECTED_PICKAXE.get())
            .add(ItemRegistry.SKY_IRON_PICKAXE.get());

        tag(ItemTags.AXES)
            .add(ItemRegistry.EARTH_ASPECTED_AXE.get())
            .add(ItemRegistry.FIRE_ASPECTED_AXE.get())
            .add(ItemRegistry.METAL_ASPECTED_AXE.get())
            .add(ItemRegistry.WATER_ASPECTED_AXE.get())
            .add(ItemRegistry.WOOD_ASPECTED_AXE.get())
            .add(ItemRegistry.SKY_IRON_AXE.get());

        tag(ItemTags.SHOVELS)
            .add(ItemRegistry.EARTH_ASPECTED_SHOVEL.get())
            .add(ItemRegistry.FIRE_ASPECTED_SHOVEL.get())
            .add(ItemRegistry.METAL_ASPECTED_SHOVEL.get())
            .add(ItemRegistry.WATER_ASPECTED_SHOVEL.get())
            .add(ItemRegistry.WOOD_ASPECTED_SHOVEL.get())
            .add(ItemRegistry.SKY_IRON_SHOVEL.get());

        tag(ItemTags.HOES)
            .add(ItemRegistry.EARTH_ASPECTED_HOE.get())
            .add(ItemRegistry.FIRE_ASPECTED_HOE.get())
            .add(ItemRegistry.METAL_ASPECTED_HOE.get())
            .add(ItemRegistry.WATER_ASPECTED_HOE.get())
            .add(ItemRegistry.WOOD_ASPECTED_HOE.get())
            .add(ItemRegistry.SKY_IRON_HOE.get());

        tag(ItemTags.HEAD_ARMOR)
            .add(ItemRegistry.EARTH_ASPECTED_HELMET.get())
            .add(ItemRegistry.FIRE_ASPECTED_HELMET.get())
            .add(ItemRegistry.METAL_ASPECTED_HELMET.get())
            .add(ItemRegistry.WATER_ASPECTED_HELMET.get())
            .add(ItemRegistry.WOOD_ASPECTED_HELMET.get())
            .add(ItemRegistry.SKY_IRON_HELMET.get());

        tag(ItemTags.CHEST_ARMOR)
            .add(ItemRegistry.EARTH_ASPECTED_CHESTPLATE.get())
            .add(ItemRegistry.FIRE_ASPECTED_CHESTPLATE.get())
            .add(ItemRegistry.METAL_ASPECTED_CHESTPLATE.get())
            .add(ItemRegistry.WATER_ASPECTED_CHESTPLATE.get())
            .add(ItemRegistry.WOOD_ASPECTED_CHESTPLATE.get())
            .add(ItemRegistry.SKY_IRON_CHESTPLATE.get());

        tag(ItemTags.LEG_ARMOR)
            .add(ItemRegistry.EARTH_ASPECTED_LEGGINGS.get())
            .add(ItemRegistry.FIRE_ASPECTED_LEGGINGS.get())
            .add(ItemRegistry.METAL_ASPECTED_LEGGINGS.get())
            .add(ItemRegistry.WATER_ASPECTED_LEGGINGS.get())
            .add(ItemRegistry.WOOD_ASPECTED_LEGGINGS.get())
            .add(ItemRegistry.SKY_IRON_LEGGINGS.get());

        tag(ItemTags.FOOT_ARMOR)
            .add(ItemRegistry.EARTH_ASPECTED_BOOTS.get())
            .add(ItemRegistry.FIRE_ASPECTED_BOOTS.get())
            .add(ItemRegistry.METAL_ASPECTED_BOOTS.get())
            .add(ItemRegistry.WATER_ASPECTED_BOOTS.get())
            .add(ItemRegistry.WOOD_ASPECTED_BOOTS.get())
            .add(ItemRegistry.SKY_IRON_BOOTS.get());
    }
}
